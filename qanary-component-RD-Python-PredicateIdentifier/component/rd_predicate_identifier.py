import os
import re
import asyncio
import logging
from difflib import SequenceMatcher

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

# SPARQL endpoint of the knowledge graph the resource lives in (the "provided
# triplestore", e.g. https://dbpedia.org/sparql). This is the graph inspected to
# discover which predicates the identified resource actually has. It is
# deliberately independent of the Qanary triplestore, so the component works
# against any endpoint that speaks SPARQL.
KNOWLEDGE_GRAPH_ENDPOINT = os.environ['KNOWLEDGE_GRAPH_ENDPOINT']

# a descriptive User-Agent is good practice (and required by some public
# endpoints such as Wikidata) to avoid being throttled
AGENT_HEADER = os.getenv(
    "AGENT_HEADER",
    "Qanary-RD-PredicateIdentifier/1.0 "
    "(https://github.com/WDAqua/Qanary-question-answering-components)")

# only predicates whose label matches the question at least this well (0..1) are
# annotated; keeps unrelated predicates of a resource from being emitted
MIN_MATCH_SCORE = float(os.getenv("MIN_MATCH_SCORE", "0.5"))

# upper bound on the number of candidate predicates fetched for one resource
MAX_PREDICATE_CANDIDATES = int(os.getenv("MAX_PREDICATE_CANDIDATES", "1000"))

router = APIRouter(
    tags=[SERVICE_NAME_COMPONENT],
    responses={404: {"description": "Not found"}},
)

# English function words that never carry the predicate meaning of a question.
STOP_WORDS = {
    "a", "an", "the", "of", "is", "are", "was", "were", "be", "been", "being",
    "what", "which", "who", "whom", "whose", "where", "when", "why", "how",
    "do", "does", "did", "in", "on", "at", "to", "for", "by", "with", "from",
    "and", "or", "as", "that", "this", "these", "those", "it", "its", "there",
    "many", "much", "list", "give", "tell", "me", "all", "name", "have", "has",
}

# namespaces that hold curated ontology predicates. When two predicates match
# the question equally well, these are preferred over free-text / raw property
# namespaces (e.g. dbo:capital is preferred over dbp:capital).
ONTOLOGY_NAMESPACE_HINTS = ("/ontology/", "wikiba.se", "/prop/direct/", "schema.org")

# Wikidata predicates are opaque (e.g. .../prop/direct/P36) and carry no
# rdfs:label on the predicate URI itself — the human-readable label lives on the
# corresponding property *entity* (wd:P36), reachable via wikibase:directClaim.
# These prefixes trigger a follow-up label lookup (see _resolve_opaque_labels).
WIKIDATA_PROPERTY_PREFIXES = (
    "http://www.wikidata.org/prop/direct/",
    "http://www.wikidata.org/prop/",
)


def local_name(uri: str) -> str:
    """The trailing identifier of a URI (the part after the last '#' or '/')."""
    return re.split(r"[#/]", uri.rstrip("#/"))[-1]


def tokenize(text: str) -> list:
    """Lower-cased, stop-word-free tokens.

    camelCase and PascalCase are split (``birthPlace`` -> ``birth place``) and
    any non-alphanumeric character (``_``, ``-``, ``.`` ...) acts as a
    separator, so both question text and predicate identifiers are reduced to
    comparable keywords.
    """
    text = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", text)
    words = re.findall(r"[A-Za-z0-9]+", text.lower())
    return [w for w in words if w not in STOP_WORDS and len(w) > 1]


def predicate_label(uri: str, rdfs_label: str = None) -> str:
    """A human-readable label for a predicate: the graph's rdfs:label if it
    provides one, otherwise the URI's local name (which is meaningful for most
    ontologies, e.g. dbo:capital -> "capital")."""
    return rdfs_label if rdfs_label else local_name(uri)


def _token_similarity(a: str, b: str) -> float:
    """1.0 for equal tokens, otherwise a character-level similarity (0..1) so
    that near-misses like "born"/"birth" still contribute."""
    if a == b:
        return 1.0
    return SequenceMatcher(None, a, b).ratio()


def match_score(label_text: str, question_tokens: list) -> float:
    """How strongly a predicate's label matches the question (0..1).

    For every label token we take its best similarity to any question token.
    The score blends the single strongest hit (a keyword such as "capital"
    matching exactly) with the average coverage of the whole label, so a label
    that matches entirely is preferred over one that only partially overlaps.
    """
    label_tokens = tokenize(label_text)
    if not label_tokens or not question_tokens:
        return 0.0
    per_label = [max(_token_similarity(lt, qt) for qt in question_tokens)
                 for lt in label_tokens]
    best = max(per_label)
    mean = sum(per_label) / len(per_label)
    return 0.7 * best + 0.3 * mean


def _is_ontology_predicate(uri: str) -> bool:
    return any(hint in uri for hint in ONTOLOGY_NAMESPACE_HINTS)


def select_best_predicate(question_text: str, candidates: list):
    """Pick the most probable predicate for the question.

    ``candidates`` is a list of ``(uri, label_or_None)`` tuples. Returns
    ``(uri, score)`` for the best match at or above ``MIN_MATCH_SCORE``, or
    ``(None, best_score)`` when nothing matches well enough.
    """
    question_tokens = tokenize(question_text)
    scored = []
    for uri, label in candidates:
        score = round(match_score(predicate_label(uri, label), question_tokens), 3)
        scored.append((score, _is_ontology_predicate(uri), uri))
    if not scored:
        return None, 0.0
    # highest score first; ties broken preferring ontology predicates, then the
    # shorter/lexicographically-first URI for deterministic output
    scored.sort(key=lambda t: (-t[0], 0 if t[1] else 1, len(t[2]), t[2]))
    best_score, _, best_uri = scored[0]
    if best_score >= MIN_MATCH_SCORE:
        return best_uri, best_score
    return None, best_score


def run_select_query(endpoint_url: str, query: str) -> list:
    """Run a SPARQL SELECT against a provided endpoint and return its bindings."""
    sparql = SPARQLWrapper(endpoint_url)
    sparql.agent = AGENT_HEADER
    sparql.setQuery(query)
    sparql.setReturnFormat(JSON)
    result = sparql.query().convert()
    return result["results"]["bindings"]


def get_identified_resources(triplestore_endpoint: str, graph: str) -> list:
    """Resources already identified by upstream components, read from the Qanary
    triplestore. Both the canonical ``qa:AnnotationOfInstance`` and the
    ``qa:AnnotationOfEntity`` variant are considered; higher-scored resources
    come first."""
    query = f"""
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        SELECT DISTINCT ?resource ?score
        FROM <{graph}>
        WHERE {{
            VALUES ?annotationType {{ qa:AnnotationOfInstance qa:AnnotationOfEntity }}
            ?annotation a ?annotationType ;
                        oa:hasBody ?resource .
            OPTIONAL {{ ?annotation qa:score ?score }}
            FILTER(isIRI(?resource))
        }}
        ORDER BY DESC(?score)
    """
    result = query_triplestore(triplestore_endpoint, query)
    resources = []
    for binding in result["results"]["bindings"]:
        resource = binding["resource"]["value"]
        if resource not in resources:
            resources.append(resource)
    return resources


def fetch_candidate_predicates(endpoint_url: str, resource: str) -> list:
    """Predicates the resource actually has in the provided knowledge graph,
    each paired with an English ``rdfs:label`` when the graph offers one.

    The labelled query is attempted first; if the endpoint rejects it or times
    out, the component falls back to a plain predicate query and relies on the
    URI local names.
    """
    labelled_query = f"""
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        SELECT DISTINCT ?p ?pLabel
        WHERE {{
            <{resource}> ?p ?o .
            OPTIONAL {{
                ?p rdfs:label ?pLabel .
                FILTER(LANG(?pLabel) = "en" || LANG(?pLabel) = "")
            }}
        }}
        LIMIT {MAX_PREDICATE_CANDIDATES}
    """
    try:
        bindings = run_select_query(endpoint_url, labelled_query)
    except Exception as error:  # noqa: BLE001 - endpoint may reject OPTIONAL join
        logging.warning(f"Labelled predicate query failed ({error}); "
                        "falling back to a plain predicate query")
        plain_query = f"""
            SELECT DISTINCT ?p
            WHERE {{ <{resource}> ?p ?o . }}
            LIMIT {MAX_PREDICATE_CANDIDATES}
        """
        bindings = run_select_query(endpoint_url, plain_query)

    predicates = {}
    for binding in bindings:
        uri = binding["p"]["value"]
        label = binding.get("pLabel", {}).get("value")
        # keep the first predicate occurrence, upgrading None -> label if a
        # later row supplies one
        if uri not in predicates or (label and not predicates[uri]):
            predicates[uri] = label

    _resolve_opaque_labels(endpoint_url, predicates)
    return list(predicates.items())


def _resolve_opaque_labels(endpoint_url: str, predicates: dict) -> None:
    """Fill in labels for opaque Wikidata property URIs (e.g. wdt:P36) in place.

    Their meaning lives on the property *entity* (wd:P36), linked to the direct
    predicate via ``wikibase:directClaim``; a plain ``?p rdfs:label`` never finds
    it. Best-effort: any failure leaves the predicate to fall back to its local
    name.
    """
    opaque = [uri for uri, label in predicates.items()
              if not label and uri.startswith(WIKIDATA_PROPERTY_PREFIXES)]
    if not opaque:
        return
    values = " ".join(f"<{uri}>" for uri in opaque)
    query = f"""
        PREFIX wikibase: <http://wikiba.se/ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        SELECT ?p ?pLabel
        WHERE {{
            VALUES ?p {{ {values} }}
            ?property wikibase:directClaim|wikibase:claim|wikibase:statementProperty ?p ;
                      rdfs:label ?pLabel .
            FILTER(LANG(?pLabel) = "en")
        }}
    """
    try:
        for binding in run_select_query(endpoint_url, query):
            uri = binding["p"]["value"]
            label = binding.get("pLabel", {}).get("value")
            if label and not predicates.get(uri):
                predicates[uri] = label
    except Exception as error:  # noqa: BLE001 - endpoint may not know wikibase:
        logging.warning(f"Wikidata label resolution failed ({error}); "
                        "keeping local names for opaque predicates")


def _relation_annotation_query(graph: str, question_uri: str, predicate: str,
                               score: float) -> str:
    """SPARQL INSERT storing the identified predicate as a qa:AnnotationOfRelation."""
    return f"""
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        INSERT {{
            GRAPH <{graph}> {{
                ?newAnnotation rdf:type qa:AnnotationOfRelation ;
                    oa:hasBody <{predicate}> ;
                    qa:score "{score}"^^xsd:float ;
                    oa:annotatedAt ?time ;
                    oa:annotatedBy <urn:qanary:{SERVICE_NAME_COMPONENT.replace(" ", "-")}> ;
                    oa:hasTarget [
                        a    oa:SpecificResource ;
                        oa:hasSource <{question_uri}> ;
                    ] .
            }}
        }}
        WHERE {{
            BIND (IRI(CONCAT("urn:qanary:annotation:relation:", STR(RAND()))) AS ?newAnnotation) .
            BIND (now() as ?time)
        }}
    """


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
    logging.info(f"Identifying predicate for question: {question_text}")

    resources = await asyncio.to_thread(
        get_identified_resources, triplestore_endpoint_url, triplestore_ingraph_uuid)
    logging.info(f"Already identified resources: {resources}")

    if not resources:
        logging.warning("No identified resource (qa:AnnotationOfInstance / "
                        "qa:AnnotationOfEntity) found in the graph; nothing to do")
        return JSONResponse(content=request_json)

    def _identify_and_store():
        for resource in resources:
            candidates = fetch_candidate_predicates(KNOWLEDGE_GRAPH_ENDPOINT, resource)
            logging.info(f"{len(candidates)} candidate predicates for <{resource}>")
            predicate, score = select_best_predicate(question_text, candidates)
            if predicate is None:
                logging.warning(f"No predicate matched the question for <{resource}> "
                                f"(best score {score:.2f})")
                continue
            logging.info(f"Most probable predicate for <{resource}>: "
                         f"<{predicate}> (score {score:.2f})")
            insert_into_triplestore(
                triplestore_endpoint_url,
                _relation_annotation_query(
                    triplestore_ingraph_uuid, question_uri, predicate, score))

    await asyncio.to_thread(_identify_and_store)
    return JSONResponse(content=request_json)


@router.get("/health")
def health():
    return PlainTextResponse(content="alive")
