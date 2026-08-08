import os
import re
import time
import asyncio
import logging

from SPARQLWrapper import SPARQLWrapper, JSON
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse, PlainTextResponse

from qanary_helpers.qanary_queries import (
    insert_into_triplestore,
    get_text_question_in_graph,
    query_triplestore,
)

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

if not os.getenv("PRODUCTION"):
    from dotenv import load_dotenv
    load_dotenv()  # required for debugging outside Docker

SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']

# SPARQL endpoint of the knowledge graph the generated queries are meant for
# (e.g. https://dbpedia.org/sparql). It is used to *validate* candidate graph
# patterns (an ASK probe per candidate) so the component can automatically find
# the matching pattern; if unset or unreachable the component still works and
# falls back to the pure rule order.
KNOWLEDGE_GRAPH_ENDPOINT = os.getenv('KNOWLEDGE_GRAPH_ENDPOINT', "")

# a descriptive User-Agent is good practice (and required by some public
# endpoints such as Wikidata) to avoid being throttled
AGENT_HEADER = os.getenv(
    "AGENT_HEADER",
    "Qanary-QB-RuleBasedQueryBuilder/1.0 "
    "(https://github.com/WDAqua/Qanary-question-answering-components)")

# how many entity/predicate annotations are consumed (the rule set covers one
# or two of each)
MAX_ANNOTATIONS_PER_TYPE = int(os.getenv("MAX_ANNOTATIONS_PER_TYPE", "2"))

# how many ranked candidate queries are stored as qa:AnnotationOfAnswerSPARQL
MAX_STORED_CANDIDATES = int(os.getenv("MAX_STORED_CANDIDATES", "3"))

# timeout (seconds) for the pattern-validation probes against the knowledge graph
KG_REQUEST_TIMEOUT = int(os.getenv("KG_REQUEST_TIMEOUT", "30"))

router = APIRouter(
    tags=[SERVICE_NAME_COMPONENT],
    responses={404: {"description": "Not found"}},
)

# lexical cues deciding the query form (inspired by the question phrasings in
# the QALD-10 and LC-QuAD benchmarks, e.g. "Was Ganymede discovered by Galileo
# Galilei?" -> ASK, "How many movies did Stanley Kubrick direct?" -> COUNT)
ASK_CUES = ("is ", "are ", "was ", "were ", "does ", "did ", "do ",
            "has ", "have ", "can ")
COUNT_CUES = ("how many", "number of", "count of")

# superlative cues select the ORDER BY direction ("highest mountain" ->
# descending by the ordering predicate's value, "shortest river" -> ascending)
SUPERLATIVE_DESC_CUES = ("highest", "tallest", "largest", "biggest", "longest",
                         "greatest", "maximum", "most")
SUPERLATIVE_ASC_CUES = ("lowest", "smallest", "shortest", "least", "minimum")

# aggregation cues select the aggregate function over the value predicate
# ("total population" -> SUM, "average elevation" -> AVG)
AGGREGATION_CUES = {"total": "SUM", "sum": "SUM", "combined": "SUM",
                    "average": "AVG", "mean": "AVG"}


def is_ask_question(question_text: str) -> bool:
    """True for yes/no questions ("Is Berlin the capital of Germany?")."""
    return question_text.lower().strip().startswith(ASK_CUES)


def is_count_question(question_text: str) -> bool:
    """True for counting questions ("How many films did Nolan direct?")."""
    lowered = question_text.lower()
    return any(cue in lowered for cue in COUNT_CUES)


def superlative_direction(question_text: str):
    """ORDER BY direction for superlative questions ("What is the highest
    mountain in Germany?" -> "DESC"), or None for non-superlative questions."""
    words = set(re.findall(r"[a-z]+", question_text.lower()))
    if words & set(SUPERLATIVE_DESC_CUES):
        return "DESC"
    if words & set(SUPERLATIVE_ASC_CUES):
        return "ASC"
    return None


def aggregation_function(question_text: str):
    """Aggregate function for value-aggregation questions ("What is the total
    population of the cities in Germany?" -> "SUM"), or None."""
    words = set(re.findall(r"[a-z]+", question_text.lower()))
    for cue, function in AGGREGATION_CUES.items():
        if cue in words:
            return function
    return None


def _select_query(triple_patterns: list, count: bool) -> str:
    where = " . ".join(triple_patterns) + " ."
    if count:
        # project to a fresh variable — re-using ?answer as the AS target is
        # invalid SPARQL (the variable is already in scope in the pattern)
        return f"SELECT (COUNT(DISTINCT ?answer) AS ?count) WHERE {{ {where} }}"
    return f"SELECT DISTINCT ?answer WHERE {{ {where} }}"


def _candidate(pattern_name: str, triple_patterns: list, form: str,
               count: bool = False, order: str = None,
               aggregate: str = None) -> dict:
    """One candidate graph pattern: the executable query, the raw WHERE
    patterns (used to build the ASK validation probe) and a pattern name.

    ``order`` ("DESC"/"ASC") appends ``ORDER BY …(?value) LIMIT 1`` for
    superlatives; ``aggregate`` ("SUM"/"AVG") projects the aggregated ?value.
    """
    where = " . ".join(triple_patterns) + " ."
    if form == "ASK":
        query = f"ASK WHERE {{ {where} }}"
    elif aggregate:
        query = f"SELECT ({aggregate}(?value) AS ?result) WHERE {{ {where} }}"
    elif order:
        query = (f"SELECT DISTINCT ?answer WHERE {{ {where} }} "
                 f"ORDER BY {order}(?value) LIMIT 1")
    else:
        query = _select_query(triple_patterns, count)
    return {"pattern": pattern_name, "query": query,
            "triple_patterns": triple_patterns, "form": form}


def _value_pattern_candidates(entities: list, predicates: list, kind: str,
                              order: str = None, aggregate: str = None) -> list:
    """Candidates whose answers carry a numeric ?value to order/aggregate by.

    One predicate relates the answer to the entity, the other provides the
    value (``?answer <p_rel> <e> . ?answer <p_ord> ?value``); both assignments
    and both relation directions are enumerated (4 variants) — the ASK probe
    against the knowledge graph then finds the matching one. Without an entity
    (e.g. "What is the highest mountain?") the single value pattern
    ``?answer <p> ?value`` is used.
    """
    candidates = []
    if not entities and len(predicates) == 1:
        candidates.append(_candidate(
            f"{kind}_global", [f"?answer <{predicates[0]}> ?value"],
            "SELECT", order=order, aggregate=aggregate))
        return candidates
    e1 = entities[0]
    for relation, value in ((predicates[0], predicates[1]),
                            (predicates[1], predicates[0])):
        for relation_backward in (True, False):
            hop = (f"?answer <{relation}> <{e1}>" if relation_backward
                   else f"<{e1}> <{relation}> ?answer")
            name = (f"{kind}_{'b' if relation_backward else 'f'}"
                    f"_{'p1p2' if relation == predicates[0] else 'p2p1'}")
            candidates.append(_candidate(
                name, [hop, f"?answer <{value}> ?value"],
                "SELECT", order=order, aggregate=aggregate))
    return candidates


def build_candidate_queries(question_text: str, entities: list,
                            predicates: list) -> list:
    """Enumerate the candidate graph patterns for the given entities and
    predicates, most probable first.

    The rule set mirrors the dominant structures of the QALD-10 and LC-QuAD
    benchmarks (E = entity, P = predicate, ? = variable):

    * 1 entity, 1 predicate:  ``E-P-?`` (forward), ``?-P-E`` (backward),
      each optionally wrapped in COUNT for "how many" questions
    * 2 entities, 1 predicate: ``ASK E-P-E`` for yes/no questions, otherwise
      the shared-predicate intersection ``?-P-E1 . ?-P-E2``
    * 1 entity, 2 predicates:  two-hop chains ``E-P1-?y . ?y-P2-?answer``
      in all direction/assignment variants; for superlative questions
      ("highest mountain in Germany") ordering patterns
      ``?answer-Prel-E . ?answer-Pval-?value ORDER BY DESC(?value) LIMIT 1``,
      and for value aggregations ("total population of the cities in Germany")
      ``SELECT (SUM(?value) AS ?result)`` over the same patterns
    * 0 entities, 1 predicate:  superlatives over the whole graph
      ("What is the highest mountain?") — ``?answer-P-?value`` ordered
    * 2 entities, 2 predicates: intersections ``?answer-P1-E1 . ?answer-P2-E2``
      in all direction/assignment variants

    All variants are enumerated; ``rank_candidates`` then probes them against
    the knowledge graph to find the actually matching pattern.
    """
    count = is_count_question(question_text)
    ask = is_ask_question(question_text)
    aggregate = None if count else aggregation_function(question_text)
    order = None if (count or aggregate) else superlative_direction(question_text)
    entities = entities[:MAX_ANNOTATIONS_PER_TYPE]
    predicates = predicates[:MAX_ANNOTATIONS_PER_TYPE]
    candidates = []

    if order and not entities and len(predicates) == 1:
        return _value_pattern_candidates([], predicates, "superlative",
                                         order=order)
    if len(entities) == 1 and len(predicates) == 2:
        if order:
            return _value_pattern_candidates(entities, predicates,
                                             "superlative", order=order)
        if aggregate:
            return _value_pattern_candidates(entities, predicates,
                                             "aggregation", aggregate=aggregate)

    if len(entities) == 1 and len(predicates) == 1:
        e1, p1 = entities[0], predicates[0]
        candidates.append(_candidate(
            "forward", [f"<{e1}> <{p1}> ?answer"], "SELECT", count))
        candidates.append(_candidate(
            "backward", [f"?answer <{p1}> <{e1}>"], "SELECT", count))

    elif len(entities) == 2 and len(predicates) == 1:
        e1, e2 = entities
        p1 = predicates[0]
        if ask:
            candidates.append(_candidate(
                "ask_forward", [f"<{e1}> <{p1}> <{e2}>"], "ASK"))
            candidates.append(_candidate(
                "ask_backward", [f"<{e2}> <{p1}> <{e1}>"], "ASK"))
        else:
            candidates.append(_candidate(
                "intersection_shared_backward",
                [f"?answer <{p1}> <{e1}>", f"?answer <{p1}> <{e2}>"],
                "SELECT", count))
            candidates.append(_candidate(
                "intersection_shared_forward",
                [f"<{e1}> <{p1}> ?answer", f"<{e2}> <{p1}> ?answer"],
                "SELECT", count))

    elif len(entities) == 1 and len(predicates) == 2:
        e1 = entities[0]
        # two-hop chain: hop 1 connects the entity to an intermediate ?joint,
        # hop 2 connects ?joint to the answer; enumerate predicate assignment
        # and both directions per hop (8 variants), most common shape first
        for first, second in ((predicates[0], predicates[1]),
                              (predicates[1], predicates[0])):
            for hop1_forward in (True, False):
                for hop2_forward in (True, False):
                    hop1 = (f"<{e1}> <{first}> ?joint" if hop1_forward
                            else f"?joint <{first}> <{e1}>")
                    hop2 = (f"?joint <{second}> ?answer" if hop2_forward
                            else f"?answer <{second}> ?joint")
                    name = (f"chain_{'f' if hop1_forward else 'b'}"
                            f"{'f' if hop2_forward else 'b'}"
                            f"_{'p1p2' if first == predicates[0] else 'p2p1'}")
                    candidates.append(_candidate(
                        name, [hop1, hop2], "SELECT", count))

    elif len(entities) == 2 and len(predicates) == 2:
        e1, e2 = entities
        # intersection: the answer is connected to both entities, one predicate
        # each; enumerate predicate-entity assignment and directions (8
        # variants), the dominant LC-QuAD shape (?-P-E1 . ?-P-E2) first
        for (pa, pb) in ((predicates[0], predicates[1]),
                         (predicates[1], predicates[0])):
            for hop1_backward in (True, False):
                for hop2_backward in (True, False):
                    hop1 = (f"?answer <{pa}> <{e1}>" if hop1_backward
                            else f"<{e1}> <{pa}> ?answer")
                    hop2 = (f"?answer <{pb}> <{e2}>" if hop2_backward
                            else f"<{e2}> <{pb}> ?answer")
                    name = (f"intersection_{'b' if hop1_backward else 'f'}"
                            f"{'b' if hop2_backward else 'f'}"
                            f"_{'p1p2' if pa == predicates[0] else 'p2p1'}")
                    candidates.append(_candidate(
                        name, [hop1, hop2], "SELECT", count))

    else:
        logging.warning(
            f"unsupported combination: {len(entities)} entities / "
            f"{len(predicates)} predicates (the rule set covers 1-2 of each)")

    return candidates


def run_ask_query(endpoint_url: str, query: str, retries: int = 3) -> bool:
    """Run a SPARQL ASK against the knowledge-graph endpoint.

    Public endpoints (DBpedia, Wikidata) rate-limit bursts of requests; an
    HTTP 429 is retried with a linear backoff instead of aborting the whole
    pattern search.
    """
    sparql = SPARQLWrapper(endpoint_url)
    sparql.agent = AGENT_HEADER
    sparql.setTimeout(KG_REQUEST_TIMEOUT)
    sparql.setQuery(query)
    sparql.setReturnFormat(JSON)
    sparql.setMethod("POST")
    for attempt in range(retries):
        try:
            return bool(sparql.query().convert().get("boolean", False))
        except Exception as error:  # noqa: BLE001 - endpoint throttling
            if "429" in str(error) and attempt < retries - 1:
                wait = 5 * (attempt + 1)
                logging.warning(f"knowledge-graph endpoint rate-limited (429); "
                                f"retrying in {wait}s")
                time.sleep(wait)
                continue
            raise


def rank_candidates(kg_endpoint: str, candidates: list) -> list:
    """Automatically search for the matching graph patterns: probe every
    candidate with an ASK against the knowledge graph and move the patterns
    that actually have matches to the front (stable order otherwise).

    For ASK candidates a ``true`` probe means the stated fact holds; if none
    holds the first orientation is kept (its ``false`` answer is the result).
    Without a knowledge-graph endpoint (or if probing fails) the pure rule
    order is returned, so the component also works offline.
    """
    if not kg_endpoint or not candidates:
        return candidates
    matching, rest = [], []
    for candidate in candidates:
        probe = f"ASK WHERE {{ {' . '.join(candidate['triple_patterns'])} . }}"
        try:
            has_match = run_ask_query(kg_endpoint, probe)
        except Exception as error:  # noqa: BLE001 - endpoint may be unreachable
            logging.warning(f"pattern validation failed ({error}); "
                            "keeping the rule-based candidate order")
            return candidates
        (matching if has_match else rest).append(candidate)
    ranked = matching + rest
    logging.info("pattern search: "
                 + ", ".join(f"{c['pattern']}={'match' if c in matching else 'no-match'}"
                             for c in ranked))
    return ranked


def escape_sparql_string(value: str) -> str:
    """Escape a string (here: a generated SPARQL query) for embedding as a
    quoted literal inside another SPARQL query."""
    return (value.replace("\\", "\\\\").replace('"', '\\"')
            .replace("\n", "\\n").replace("\r", ""))


def _answer_sparql_annotation_query(graph: str, question_uri: str,
                                    answer_sparql: str, score: float) -> str:
    """SPARQL INSERT storing one generated query as qa:AnnotationOfAnswerSPARQL."""
    return f"""
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        INSERT {{
            GRAPH <{graph}> {{
                ?newAnnotation rdf:type qa:AnnotationOfAnswerSPARQL ;
                    oa:hasTarget <{question_uri}> ;
                    oa:hasBody "{escape_sparql_string(answer_sparql)}" ;
                    qa:score "{score}"^^xsd:float ;
                    oa:annotatedAt ?time ;
                    oa:annotatedBy <urn:qanary:{SERVICE_NAME_COMPONENT.replace(" ", "-")}> .
            }}
        }}
        WHERE {{
            BIND (IRI(CONCAT("urn:qanary:annotation:answer:sparql:", STR(RAND()))) AS ?newAnnotation) .
            BIND (now() as ?time) .
        }}
    """


def get_annotated_entities(triplestore_endpoint: str, graph: str) -> list:
    """Entity URIs already identified by upstream components (NED/NEL), read
    from the Qanary triplestore; both the canonical qa:AnnotationOfInstance and
    the qa:AnnotationOfEntity variant are considered, highest score first."""
    query = f"""
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        SELECT ?body ?score
        FROM <{graph}>
        WHERE {{
            VALUES ?annotationType {{ qa:AnnotationOfInstance qa:AnnotationOfEntity }}
            ?annotation a ?annotationType ;
                        oa:hasBody ?body .
            OPTIONAL {{ ?annotation qa:score ?score }}
            FILTER(isIRI(?body))
        }}
        ORDER BY DESC(?score)
    """
    return _deduped_bodies(query_triplestore(triplestore_endpoint, query))


def get_annotated_predicates(triplestore_endpoint: str, graph: str) -> list:
    """Predicate URIs already identified by upstream components (relation
    linking), read from the Qanary triplestore, highest score first."""
    query = f"""
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        SELECT ?body ?score
        FROM <{graph}>
        WHERE {{
            ?annotation a qa:AnnotationOfRelation ;
                        oa:hasBody ?body .
            OPTIONAL {{ ?annotation qa:score ?score }}
            FILTER(isIRI(?body))
        }}
        ORDER BY DESC(?score)
    """
    return _deduped_bodies(query_triplestore(triplestore_endpoint, query))


def _deduped_bodies(result: dict) -> list:
    bodies = []
    for binding in result["results"]["bindings"]:
        value = binding["body"]["value"]
        if value not in bodies:
            bodies.append(value)
    return bodies


@router.get("/")
async def service_description(request: Request):
    return JSONResponse(content={"name": SERVICE_NAME_COMPONENT})


@router.post("/annotatequestion")
async def annotate_question(request: Request):
    request_json = await request.json()
    triplestore_endpoint_url = request_json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph_uuid = request_json["values"]["urn:qanary#inGraph"]

    # All triplestore and knowledge-graph I/O below is blocking. Run it off the
    # event loop via asyncio.to_thread so this worker keeps serving /health and
    # the registration heartbeat while a slow endpoint responds — otherwise the
    # component is marked unhealthy and dropped OFFLINE (and the synchronous
    # pipeline call hangs).
    question = (await asyncio.to_thread(
        get_text_question_in_graph,
        triplestore_endpoint=triplestore_endpoint_url,
        graph=triplestore_ingraph_uuid))[0]
    question_text, question_uri = question["text"], question["uri"]
    logging.info(f"Building query for question: {question_text}")

    entities = (await asyncio.to_thread(
        get_annotated_entities, triplestore_endpoint_url,
        triplestore_ingraph_uuid))[:MAX_ANNOTATIONS_PER_TYPE]
    predicates = (await asyncio.to_thread(
        get_annotated_predicates, triplestore_endpoint_url,
        triplestore_ingraph_uuid))[:MAX_ANNOTATIONS_PER_TYPE]
    logging.info(f"entities: {entities}; predicates: {predicates}")

    # entities may be empty for global superlatives ("What is the highest
    # mountain?"); build_candidate_queries decides which combinations work
    if not predicates:
        logging.warning("no predicate annotations in the graph; nothing to do")
        return JSONResponse(content=request_json)

    candidates = build_candidate_queries(question_text, entities, predicates)
    if not candidates:
        logging.warning("no candidate patterns for this entity/predicate "
                        "combination; nothing to do")
        return JSONResponse(content=request_json)

    ranked = await asyncio.to_thread(
        rank_candidates, KNOWLEDGE_GRAPH_ENDPOINT, candidates)

    def _store_candidates():
        for index, candidate in enumerate(ranked[:MAX_STORED_CANDIDATES]):
            score = round(max(1.0 - 0.1 * index, 0.1), 2)
            logging.info(f"storing candidate {candidate['pattern']} "
                         f"(score {score}): {candidate['query']}")
            insert_into_triplestore(
                triplestore_endpoint_url,
                _answer_sparql_annotation_query(
                    triplestore_ingraph_uuid, question_uri,
                    candidate["query"], score))

    await asyncio.to_thread(_store_candidates)
    return JSONResponse(content=request_json)


@router.get("/health")
def health():
    return PlainTextResponse(content="alive")
