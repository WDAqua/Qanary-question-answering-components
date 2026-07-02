"""Live quality test against the QALD-9 test benchmark (opt-in).

Independent of the other suites and **disabled by default** (the regular test
pipeline stays offline): activate with ``QALD9_LIVE_TEST_ACTIVE=true``.

For every QALD-9 test question whose gold SPARQL query lies within the
component's scope (1-2 DBpedia entities x 1-2 predicates, no
FILTER/UNION/... constructs), the full component pipeline runs with the *live*
DBpedia endpoint for pattern validation. Then **both** the gold query and the
generated query are executed on the same live endpoint, so the two result sets
are directly comparable (the DBpedia release has moved on since QALD-9 was
published — executing both queries today is the fair comparison; the answer
sets shipped with the benchmark are not used). Per question,
precision/recall/F1 are computed over the result sets; an extensive comparison
report (aggregate metrics plus a per-question breakdown with both queries and
both result sets) is written to a temporary file whose path appears in the
pytest output and in the run report.

The quality is expectedly below 100% — gold queries may use class constraints
(``rdf:type``) or vocabulary outside the annotated entity/predicate pairs. The
test only asserts a configurable floor (``QALD9_MIN_F1``, default 0.3 macro-F1
over the attempted questions).

Environment:
* ``QALD9_LIVE_TEST_ACTIVE`` -- set ``true`` to enable this suite
* ``QALD9_LIMIT``            -- run only the first N in-scope questions (0 = all)
* ``QALD9_MIN_F1``           -- macro-F1 floor for the assertion (default 0.3)
* ``QALD9_REPORT_FILE``      -- fixed path for the comparison report
"""
import os
import re
import json
import time
import tempfile
import urllib.request

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from SPARQLWrapper import SPARQLWrapper, JSON

from tests.sparql_server import OfflineSparqlServer
from tests.pipeline_utils import seed_annotations, read_best_query

LIVE_TEST_ACTIVE = os.getenv("QALD9_LIVE_TEST_ACTIVE", "false").lower() == "true"
pytestmark = pytest.mark.skipif(
    not LIVE_TEST_ACTIVE,
    reason="live QALD-9 quality test disabled (set QALD9_LIVE_TEST_ACTIVE=true)")

QALD9_TEST_URL = ("https://raw.githubusercontent.com/ag-sc/QALD/master/9/data/"
                  "qald-9-test-multilingual.json")
DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql"
UNSUPPORTED = re.compile(r"\b(FILTER|UNION|GROUP BY|HAVING|MINUS|OFFSET)\b", re.I)
AGENT = ("Qanary-QB-RuleBasedQueryBuilder/1.0 QALD-9 quality test "
         "(https://github.com/WDAqua/Qanary-question-answering-components)")


# --------------------------------------------------------------------------- #
# gold-query analysis
# --------------------------------------------------------------------------- #
def extract_gold_terms(sparql: str):
    """Entities and predicates used by a gold query, in order of appearance.

    This emulates perfect upstream NED/REL components: the query builder is
    evaluated in isolation, fed exactly the entities/predicates the gold query
    uses (the standard setup for testing a QB component alone).
    """
    condensed = " ".join(sparql.split())
    prefixes = dict(re.findall(r"PREFIX\s+(\w+):\s*<([^>]+)>", condensed, re.I))
    where = condensed[condensed.upper().find("WHERE"):] \
        if "WHERE" in condensed.upper() else condensed
    occurrences = []
    for match in re.finditer(r"<(http://[^>]+)>", where):
        occurrences.append((match.start(), match.group(1)))
    for match in re.finditer(r"\b(\w+):([A-Za-z0-9_\.\-\(\)\']+)", where):
        if match.group(1) in prefixes:
            occurrences.append((match.start(),
                                prefixes[match.group(1)] + match.group(2)))
    entities, predicates = [], []
    for _, iri in sorted(occurrences):
        if "/resource/" in iri and iri not in entities:
            entities.append(iri)
        elif (("/ontology/" in iri or "/property/" in iri)
              and iri not in predicates):
            predicates.append(iri)
    return entities, predicates


def in_scope(sparql: str, entities: list, predicates: list) -> bool:
    return (not UNSUPPORTED.search(sparql)
            and 1 <= len(entities) <= 2 and 1 <= len(predicates) <= 2)


# --------------------------------------------------------------------------- #
# live execution and comparison
# --------------------------------------------------------------------------- #
def execute_live(query: str, retries: int = 4):
    """Execute a query on DBpedia; normalised result set or an error marker.

    Rate limiting (HTTP 429) is retried with a linear backoff so throttling
    does not distort the quality measurement.
    """
    sparql = SPARQLWrapper(DBPEDIA_ENDPOINT)
    sparql.agent = AGENT
    sparql.setTimeout(60)
    sparql.setQuery(query)
    sparql.setReturnFormat(JSON)
    result = None
    for attempt in range(retries):
        try:
            result = sparql.query().convert()
            break
        except Exception as error:  # noqa: BLE001 - live endpoint may refuse
            if "429" in str(error) and attempt < retries - 1:
                time.sleep(6 * (attempt + 1))
                continue
            return {"error": str(error)[:200]}
    if "boolean" in result:
        return {"values": {str(result["boolean"]).lower()}}
    values = set()
    for binding in result.get("results", {}).get("bindings", []):
        for term in binding.values():
            values.add(_normalise(term.get("value", "")))
    return {"values": values}


def _normalise(value: str) -> str:
    try:  # align numeric spellings ("42" vs "42.0")
        return repr(round(float(value), 6))
    except ValueError:
        return value


def precision_recall_f1(gold: set, system: set):
    if not gold and not system:
        return 1.0, 1.0, 1.0
    overlap = len(gold & system)
    precision = overlap / len(system) if system else 0.0
    recall = overlap / len(gold) if gold else 0.0
    f1 = (2 * precision * recall / (precision + recall)
          if precision + recall else 0.0)
    return precision, recall, f1


# --------------------------------------------------------------------------- #
# the test
# --------------------------------------------------------------------------- #
def load_qald9():
    cached = os.path.join(tempfile.gettempdir(), "qald-9-test-multilingual.json")
    if not os.path.exists(cached):
        with urllib.request.urlopen(QALD9_TEST_URL, timeout=60) as response:
            data = response.read()
        with open(cached, "wb") as handle:
            handle.write(data)
    with open(cached, encoding="utf-8") as handle:
        return json.load(handle)["questions"]


def question_text(question: dict) -> str:
    return next((entry["string"] for entry in question["question"]
                 if entry["language"] == "en"), "")


def test_qald9_quality_against_gold_standard(request):
    questions = load_qald9()
    limit = int(os.getenv("QALD9_LIMIT", "0"))

    # component wired to the live DBpedia endpoint for pattern validation;
    # the offline server only plays the Qanary triplestore
    with OfflineSparqlServer(os.path.join(os.path.dirname(__file__),
                                          "fixtures", "offline_kg.ttl")) as server:
        os.environ["KNOWLEDGE_GRAPH_ENDPOINT"] = DBPEDIA_ENDPOINT
        import importlib
        from component import qb_rule_based
        importlib.reload(qb_rule_based)
        app = FastAPI()
        app.include_router(qb_rule_based.router)
        client = TestClient(app)

        rows, skipped = [], []
        for question in questions:
            text = question_text(question)
            gold_sparql = " ".join(question["query"].get("sparql", "").split())
            entities, predicates = extract_gold_terms(gold_sparql)
            if not text or not gold_sparql \
                    or not in_scope(gold_sparql, entities, predicates):
                skipped.append({"id": question["id"], "question": text,
                                "reason": "outside the 1-2 entity / 1-2 "
                                          "predicate rule scope"})
                continue
            if limit and len(rows) >= limit:
                break

            graph = f"urn:qanary:qald9:{question['id']}"
            seed_annotations(server, graph, f"qald9-{question['id']}", text,
                             entities, predicates)
            response = client.post("/annotatequestion", json={"values": {
                "urn:qanary#endpoint": server.triplestore_endpoint,
                "urn:qanary#inGraph": graph,
                "urn:qanary#outGraph": graph,
            }})
            assert response.status_code == 200
            generated = read_best_query(server, graph)

            gold_result = execute_live(gold_sparql)
            system_result = (execute_live(generated) if generated
                             else {"values": set()})
            error = gold_result.get("error") or system_result.get("error")
            if error:
                precision = recall = f1 = 0.0
            else:
                precision, recall, f1 = precision_recall_f1(
                    gold_result["values"], system_result["values"])
            rows.append({
                "id": question["id"], "question": text,
                "entities": entities, "predicates": predicates,
                "gold": gold_sparql, "generated": generated,
                "gold_n": len(gold_result.get("values", [])),
                "system_n": len(system_result.get("values", [])),
                "overlap": len(gold_result.get("values", set())
                               & system_result.get("values", set())),
                "precision": precision, "recall": recall, "f1": f1,
                "error": error,
            })
            time.sleep(1.0)  # be polite to the public endpoint (avoid 429s)
    os.environ["KNOWLEDGE_GRAPH_ENDPOINT"] = ""

    report_path = write_report(rows, skipped, len(questions))
    attempted = len(rows)
    macro_f1 = sum(row["f1"] for row in rows) / attempted if attempted else 0.0
    exact = sum(1 for row in rows if row["f1"] == 1.0)
    request.node._kg_report_line = (
        f"QALD-9 live quality: attempted={attempted} exact={exact} "
        f"macro-F1={macro_f1:.3f}  |  full comparison -> {report_path}")

    minimum_f1 = float(os.getenv("QALD9_MIN_F1", "0.3"))
    assert attempted >= 20, "too few in-scope QALD-9 questions were attempted"
    assert macro_f1 >= minimum_f1, (
        f"macro-F1 {macro_f1:.3f} fell below the {minimum_f1} floor — "
        f"see the full comparison report: {report_path}")


def write_report(rows, skipped, total):
    """The extensive component-vs-gold-standard comparison (markdown)."""
    path = os.environ.get("QALD9_REPORT_FILE") or os.path.join(
        tempfile.gettempdir(),
        f"qald9-comparison-{time.strftime('%Y%m%d-%H%M%S')}.md")
    attempted = len(rows)
    exact = sum(1 for r in rows if r["f1"] == 1.0)
    partial = sum(1 for r in rows if 0.0 < r["f1"] < 1.0)
    zero = sum(1 for r in rows if r["f1"] == 0.0 and not r["error"])
    errors = sum(1 for r in rows if r["error"])
    def macro(metric):
        return sum(r[metric] for r in rows) / attempted if attempted else 0.0
    with open(path, "w", encoding="utf-8") as f:
        f.write("# QALD-9 test set: rule-based query builder vs. gold standard\n\n")
        f.write(f"generated: {time.strftime('%Y-%m-%d %H:%M:%S')}  \n")
        f.write(f"endpoint: {DBPEDIA_ENDPOINT} (both the gold and the generated "
                "query were executed live, so both face today's DBpedia)\n\n")
        f.write("## Aggregate results\n\n")
        f.write("| metric | value |\n|---|---|\n")
        f.write(f"| QALD-9 test questions | {total} |\n")
        f.write(f"| in scope (1-2 entities x 1-2 predicates) & attempted | {attempted} |\n")
        f.write(f"| out of scope | {len(skipped)} |\n")
        f.write(f"| exact matches (F1 = 1.0) | {exact} |\n")
        f.write(f"| partial matches (0 < F1 < 1) | {partial} |\n")
        f.write(f"| no overlap (F1 = 0) | {zero} |\n")
        f.write(f"| execution errors | {errors} |\n")
        f.write(f"| macro precision | {macro('precision'):.3f} |\n")
        f.write(f"| macro recall | {macro('recall'):.3f} |\n")
        f.write(f"| **macro F1** | **{macro('f1'):.3f}** |\n\n")
        f.write("## Per-question comparison\n\n")
        for r in sorted(rows, key=lambda r: -r["f1"]):
            marker = ("✔ exact" if r["f1"] == 1.0 else
                      "~ partial" if r["f1"] > 0 else
                      "! error" if r["error"] else "✘ no overlap")
            f.write(f"### {marker} — QALD-9 #{r['id']}: {r['question']}\n\n")
            f.write(f"* entities: {', '.join(r['entities'])}\n")
            f.write(f"* predicates: {', '.join(r['predicates'])}\n")
            f.write(f"* gold query: `{r['gold']}`\n")
            f.write(f"* generated query: `{r['generated'] or '(none)'}`\n")
            f.write(f"* result sets: gold={r['gold_n']} system={r['system_n']} "
                    f"overlap={r['overlap']}\n")
            f.write(f"* precision={r['precision']:.3f} recall={r['recall']:.3f} "
                    f"**F1={r['f1']:.3f}**")
            if r["error"]:
                f.write(f"  (error: {r['error']})")
            f.write("\n\n")
        f.write("## Out-of-scope questions\n\n")
        for s in skipped:
            f.write(f"* #{s['id']} {s['question']} — {s['reason']}\n")
    return path
