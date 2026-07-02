"""Shared harness for the per-knowledge-graph predicate-identification suites.

Every case has the same shape: the input is a **natural-language question** and
a **resource URI**, and the expected output is a **predicate URI**. Given the
resource URI, the component reads that resource's predicate inventory from the
provided triplestore and ranks it against the question; here the inventory is
served from the committed fixture (``kg_fixtures/<kg>.json``), so the suites are
fully offline and deterministic.

In verbose mode each case prints ``resource | question -> predicate`` with an
ok/failed symbol — see ``record_report_line`` and the hooks in ``conftest.py``.
"""
import os
import json
import importlib

FIXTURE_DIR = os.path.join(os.path.dirname(__file__), "kg_fixtures")


def load_component():
    """Import the component module with a stable configuration.

    The module reads its config at import time, so we set the environment first
    and reload to avoid depending on how a previous import was configured.
    """
    os.environ.setdefault("PRODUCTION", "True")
    os.environ.setdefault("SERVICE_NAME_COMPONENT", "RD-Python-PredicateIdentifier")
    os.environ.setdefault("KNOWLEDGE_GRAPH_ENDPOINT", "https://dbpedia.org/sparql")
    os.environ.setdefault("MIN_MATCH_SCORE", "0.5")
    from component import rd_predicate_identifier
    return importlib.reload(rd_predicate_identifier)


def load_fixture(name):
    with open(os.path.join(FIXTURE_DIR, f"{name}.json"), encoding="utf-8") as handle:
        return json.load(handle)


def load_kg(name):
    """Return ``(candidates_by_resource, cases, ids)`` for a knowledge graph.

    * ``candidates_by_resource`` maps a resource URI to the predicate inventory
      the component reads from the provided triplestore (stands in for the live
      ``fetch_candidate_predicates`` call).
    * ``cases`` is a list of ``(resource_uri, question, expected_predicate)``.
    * ``ids`` are the matching, human-readable pytest parameter ids.
    """
    fixture = load_fixture(name)
    candidates_by_resource, cases, ids = {}, [], []
    for resource in fixture["resources"]:
        candidates_by_resource[resource["resource"]] = [
            tuple(candidate) for candidate in resource["candidates"]]
        label = resource["resource_label"]
        for index, case in enumerate(resource["cases"]):
            cases.append((resource["resource"], case["question"],
                          case["expected_predicate"]))
            ids.append(f"{label}#{index}")
    return candidates_by_resource, cases, ids


def identify_predicate(component, candidates_by_resource, resource_uri, question):
    """The component's task: from a question and a resource URI, return the
    predicate URI (and its score).

    The resource's predicate inventory is looked up as it would be read from the
    provided triplestore, then ranked against the question.
    """
    candidates = candidates_by_resource[resource_uri]
    return component.select_best_predicate(question, candidates)


def record_report_line(request, resource_uri, question, predicate):
    """Stash the ``resource | question -> predicate`` line so the pytest hooks
    (see ``conftest.py``) can print it with an ok/failed symbol in verbose mode."""
    request.node._kg_report_line = f"{resource_uri}  |  {question}  ->  {predicate}"


def failure_message(resource_uri, question, expected, predicate, score):
    return (f"resource {resource_uri}\n  question: {question!r}\n"
            f"  expected: {expected}\n  got:      {predicate} (score {score})")


def fixture_summary(name):
    fixture = load_fixture(name)
    return fixture["resource_count"], fixture["case_count"]
