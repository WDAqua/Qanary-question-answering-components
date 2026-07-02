"""Offline end-to-end tests over 100+ benchmark-inspired questions.

For every question in ``fixtures/questions.json`` the full component pipeline
is exercised, without any external network access:

1. an offline Qanary triplestore (rdflib served over local HTTP, see
   ``sparql_server.py``) is seeded with the question and its entity/predicate
   annotations — exactly what NED/NEL and relation-linking components leave
   behind in a real pipeline run;
2. the component's ``POST /annotatequestion`` endpoint is called with that
   triplestore/graph, so the component reads the annotations over SPARQL,
   builds candidate graph patterns, validates them against the offline
   knowledge-graph endpoint and writes ``qa:AnnotationOfAnswerSPARQL`` back;
3. the highest-scored generated query is read from the triplestore, executed
   against the offline knowledge graph (``fixtures/offline_kg.ttl``), and its
   results are compared with the expected gold answers.

Run ``pytest -v`` to see, per question, the input entities, the question and
the generated query with an ok/failed symbol.
"""
import os
import json
import importlib

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from tests.sparql_server import OfflineSparqlServer
from tests.pipeline_utils import seed_annotations, read_best_query

FIXTURES = os.path.join(os.path.dirname(__file__), "fixtures")


def load_questions():
    with open(os.path.join(FIXTURES, "questions.json"), encoding="utf-8") as f:
        bank = json.load(f)
    prefixes = bank["prefixes"]

    def expand(curie):
        prefix, _, local = curie.partition(":")
        return prefixes[prefix] + local

    questions = []
    for entry in bank["questions"]:
        expanded = dict(entry)
        expanded["entities"] = [expand(e) for e in entry["entities"]]
        expanded["predicates"] = [expand(p) for p in entry["predicates"]]
        if "uris" in entry["expected"]:
            expanded["expected"] = {"uris": {expand(u) for u in entry["expected"]["uris"]}}
        questions.append(expanded)
    return questions


QUESTIONS = load_questions()
IDS = [f"{q['id']}-{q['type']}" for q in QUESTIONS]


@pytest.fixture(scope="module")
def pipeline():
    """The offline pipeline: SPARQL server + component wired to it."""
    with OfflineSparqlServer(os.path.join(FIXTURES, "offline_kg.ttl")) as server:
        # the component reads its configuration at import time, so point it at
        # the offline knowledge-graph endpoint and reload
        os.environ["KNOWLEDGE_GRAPH_ENDPOINT"] = server.knowledge_graph_endpoint
        from component import qb_rule_based
        importlib.reload(qb_rule_based)
        app = FastAPI()
        app.include_router(qb_rule_based.router)
        yield server, TestClient(app)
    os.environ["KNOWLEDGE_GRAPH_ENDPOINT"] = ""


def execute_on_kg(server, sparql):
    """Execute a generated query against the offline knowledge graph."""
    result = server.knowledge_graph.query(sparql)
    if result.type == "ASK":
        return {"boolean": result.askAnswer}
    values = [row[0] for row in result]
    if len(values) == 1:
        try:
            # COUNT/SUM/AVG projections yield a single numeric literal
            number = float(values[0])
            return {"count": int(number), "number": number,
                    "uris": {str(v) for v in values}}
        except (ValueError, TypeError):
            pass
    return {"uris": {str(v) for v in values}}


@pytest.mark.parametrize("question", QUESTIONS, ids=IDS)
def test_e2e_question(question, pipeline, request):
    server, client = pipeline
    graph = f"urn:qanary:graph:{question['id']}"
    seed_annotations(server, graph, question["id"], question["question"],
                     question["entities"], question["predicates"])

    # run the component against the offline triplestore (full HTTP round trips)
    response = client.post("/annotatequestion", json={"values": {
        "urn:qanary#endpoint": server.triplestore_endpoint,
        "urn:qanary#inGraph": graph,
        "urn:qanary#outGraph": graph,
    }})
    assert response.status_code == 200

    generated = read_best_query(server, graph)
    request.node._kg_report_line = (
        f"{', '.join(question['entities'])}  |  {question['question']}  ->  "
        f"{' '.join((generated or 'NO QUERY').split())}")
    assert generated, f"no qa:AnnotationOfAnswerSPARQL stored for {question['id']}"

    actual = execute_on_kg(server, generated)
    expected = question["expected"]
    if "boolean" in expected:
        assert actual.get("boolean") == expected["boolean"], (
            f"{question['question']}\n  query: {generated}\n  got {actual}")
    elif "count" in expected:
        assert actual.get("count") == expected["count"], (
            f"{question['question']}\n  query: {generated}\n  got {actual}")
    elif "number" in expected:
        assert actual.get("number") == pytest.approx(expected["number"]), (
            f"{question['question']}\n  query: {generated}\n  got {actual}")
    else:
        assert actual.get("uris") == expected["uris"], (
            f"{question['question']}\n  query: {generated}\n  got {actual}")


def test_question_bank_is_large_and_diverse():
    assert len(QUESTIONS) >= 100
    types = {q["type"] for q in QUESTIONS}
    assert {"forward", "backward", "ask", "count", "chain", "intersection",
            "intersection_shared", "superlative", "aggregation"} <= types
    # different numbers of entities and predicates are covered
    combinations = {(len(q["entities"]), len(q["predicates"])) for q in QUESTIONS}
    assert {(1, 1), (2, 1), (1, 2), (2, 2), (0, 1)} <= combinations
    assert len({q["id"] for q in QUESTIONS}) == len(QUESTIONS)
