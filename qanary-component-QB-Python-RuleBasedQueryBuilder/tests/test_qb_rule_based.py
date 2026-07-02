"""Unit tests for the rule-based query builder (network-free, all I/O mocked)."""
import importlib
from unittest import mock
from unittest import TestCase

from fastapi import FastAPI
from fastapi.testclient import TestClient


def build_client():
    """(re)import the component module and wrap its router in a throw-away app."""
    from component import qb_rule_based
    importlib.reload(qb_rule_based)
    app = FastAPI()
    app.include_router(qb_rule_based.router)
    return qb_rule_based, TestClient(app)


E1 = "http://dbpedia.org/resource/Germany"
E2 = "http://dbpedia.org/resource/Berlin"
P1 = "http://dbpedia.org/ontology/capital"
P2 = "http://dbpedia.org/ontology/mayor"

REQUEST_DATA = {
    "values": {
        "urn:qanary#endpoint": "urn:qanary#test-endpoint",
        "urn:qanary#inGraph": "urn:qanary#test-inGraph",
        "urn:qanary#outGraph": "urn:qanary#test-outGraph",
    }
}


class TestCues(TestCase):

    def test_ask_cue(self):
        module, _ = build_client()
        assert module.is_ask_question("Is Berlin the capital of Germany?")
        assert module.is_ask_question("Did Nolan direct Inception?")
        assert not module.is_ask_question("What is the capital of Germany?")

    def test_count_cue(self):
        module, _ = build_client()
        assert module.is_count_question("How many films did Nolan direct?")
        assert module.is_count_question("What is the number of rivers in Germany?")
        assert not module.is_count_question("Which films did Nolan direct?")

    def test_superlative_cue(self):
        module, _ = build_client()
        assert module.superlative_direction("What is the highest mountain?") == "DESC"
        assert module.superlative_direction("the most populated city in France") == "DESC"
        assert module.superlative_direction("What is the shortest river?") == "ASC"
        assert module.superlative_direction("What is the capital of Germany?") is None

    def test_aggregation_cue(self):
        module, _ = build_client()
        assert module.aggregation_function("What is the total population of Germany's cities?") == "SUM"
        assert module.aggregation_function("What is the average elevation of the Alps?") == "AVG"
        assert module.aggregation_function("What is the population of Berlin?") is None


class TestCandidateEnumeration(TestCase):

    def test_one_entity_one_predicate(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("What is the capital of Germany?",
                                                    [E1], [P1])
        assert [c["pattern"] for c in candidates] == ["forward", "backward"]
        assert candidates[0]["query"] == \
            f"SELECT DISTINCT ?answer WHERE {{ <{E1}> <{P1}> ?answer . }}"
        assert candidates[1]["query"] == \
            f"SELECT DISTINCT ?answer WHERE {{ ?answer <{P1}> <{E1}> . }}"

    def test_count_wrapping(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("How many rivers cross Germany?",
                                                    [E1], [P1])
        assert all("SELECT (COUNT(DISTINCT ?answer) AS ?count)" in c["query"]
                   for c in candidates)

    def test_two_entities_one_predicate_ask(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("Is Berlin the capital of Germany?",
                                                    [E2, E1], [P1])
        assert [c["pattern"] for c in candidates] == ["ask_forward", "ask_backward"]
        assert candidates[0]["query"] == f"ASK WHERE {{ <{E2}> <{P1}> <{E1}> . }}"
        assert candidates[1]["query"] == f"ASK WHERE {{ <{E1}> <{P1}> <{E2}> . }}"

    def test_two_entities_one_predicate_intersection(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("Which river crosses Germany and Berlin?",
                                                    [E1, E2], [P1])
        assert [c["pattern"] for c in candidates] == [
            "intersection_shared_backward", "intersection_shared_forward"]
        assert f"?answer <{P1}> <{E1}>" in candidates[0]["query"]
        assert f"?answer <{P1}> <{E2}>" in candidates[0]["query"]

    def test_one_entity_two_predicates_chain(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("Who is the mayor of the capital of Germany?",
                                                    [E1], [P1, P2])
        assert len(candidates) == 8  # 2 assignments x 2 x 2 directions
        assert candidates[0]["pattern"] == "chain_ff_p1p2"
        assert candidates[0]["query"] == (
            "SELECT DISTINCT ?answer WHERE { "
            f"<{E1}> <{P1}> ?joint . ?joint <{P2}> ?answer . }}")
        assert len({c["query"] for c in candidates}) == 8  # all distinct

    def test_two_entities_two_predicates_intersection(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("Which scientist was born in X and died in Y?",
                                                    [E1, E2], [P1, P2])
        assert len(candidates) == 8
        assert candidates[0]["pattern"] == "intersection_bb_p1p2"
        assert candidates[0]["query"] == (
            "SELECT DISTINCT ?answer WHERE { "
            f"?answer <{P1}> <{E1}> . ?answer <{P2}> <{E2}> . }}")

    def test_superlative_one_entity_two_predicates(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries(
            "What is the highest mountain in Germany?", [E1], [P1, P2])
        # 2 predicate assignments x 2 relation directions
        assert len(candidates) == 4
        assert candidates[0]["pattern"] == "superlative_b_p1p2"
        assert candidates[0]["query"] == (
            "SELECT DISTINCT ?answer WHERE { "
            f"?answer <{P1}> <{E1}> . ?answer <{P2}> ?value . }} "
            "ORDER BY DESC(?value) LIMIT 1")

    def test_superlative_ascending(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries(
            "What is the shortest river crossing Germany?", [E1], [P1, P2])
        assert all("ORDER BY ASC(?value) LIMIT 1" in c["query"] for c in candidates)

    def test_superlative_without_entity(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries(
            "What is the highest mountain?", [], [P1])
        assert len(candidates) == 1
        assert candidates[0]["pattern"] == "superlative_global"
        assert candidates[0]["query"] == (
            f"SELECT DISTINCT ?answer WHERE {{ ?answer <{P1}> ?value . }} "
            "ORDER BY DESC(?value) LIMIT 1")

    def test_aggregation_sum_and_avg(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries(
            "What is the total population of the cities in Germany?", [E1], [P1, P2])
        assert len(candidates) == 4
        assert all("SELECT (SUM(?value) AS ?result)" in c["query"] for c in candidates)
        candidates = module.build_candidate_queries(
            "What is the average elevation of the mountains in Switzerland?",
            [E1], [P1, P2])
        assert all("SELECT (AVG(?value) AS ?result)" in c["query"] for c in candidates)

    def test_count_cue_wins_over_superlative_and_aggregation(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries(
            "How many of the largest cities have a total population?", [E1], [P1])
        assert all("COUNT" in c["query"] for c in candidates)

    def test_unsupported_combinations_return_empty(self):
        module, _ = build_client()
        assert module.build_candidate_queries("q", [], [P1]) == []
        assert module.build_candidate_queries("q", [E1], []) == []


class TestRanking(TestCase):

    def test_matching_patterns_move_to_front(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("What is the capital of Germany?",
                                                    [E1], [P1])
        # forward has no match, backward has one -> backward must come first
        with mock.patch.object(module, "run_ask_query",
                               side_effect=[False, True]):
            ranked = module.rank_candidates("http://kg.example/sparql", candidates)
        assert [c["pattern"] for c in ranked] == ["backward", "forward"]

    def test_probe_failure_keeps_rule_order(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("What is the capital of Germany?",
                                                    [E1], [P1])
        with mock.patch.object(module, "run_ask_query",
                               side_effect=Exception("endpoint down")):
            ranked = module.rank_candidates("http://kg.example/sparql", candidates)
        assert [c["pattern"] for c in ranked] == ["forward", "backward"]

    def test_no_endpoint_keeps_rule_order(self):
        module, _ = build_client()
        candidates = module.build_candidate_queries("What is the capital of Germany?",
                                                    [E1], [P1])
        assert module.rank_candidates("", candidates) == candidates


class TestEscaping(TestCase):

    def test_escape_sparql_string(self):
        module, _ = build_client()
        assert module.escape_sparql_string('a "quoted" query\nline') == \
            'a \\"quoted\\" query\\nline'
        assert module.escape_sparql_string("back\\slash") == "back\\\\slash"

    def test_annotation_query_embeds_escaped_query(self):
        module, _ = build_client()
        annotation = module._answer_sparql_annotation_query(
            "urn:g", "urn:q", 'SELECT ?x WHERE { ?x a "type" }', 1.0)
        assert 'SELECT ?x WHERE { ?x a \\"type\\" }' in annotation
        assert "AnnotationOfAnswerSPARQL" in annotation
        assert "urn:qanary:QB-Python-RuleBasedQueryBuilder" in annotation


class TestAnnotateQuestion(TestCase):

    def test_health_endpoint(self):
        _, client = build_client()
        response = client.get("/health")
        assert response.status_code == 200
        assert response.text == "alive"

    def test_stores_ranked_candidates(self):
        module, client = build_client()
        with mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "What is the capital of Germany?",
                                              "uri": "urn:qanary#question-1"}]), \
             mock.patch.object(module, "get_annotated_entities", return_value=[E1]), \
             mock.patch.object(module, "get_annotated_predicates", return_value=[P1]), \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert response.json() == REQUEST_DATA
        # both candidates stored (MAX_STORED_CANDIDATES=3 > 2 available)
        assert mocked_insert.call_count == 2
        first_insert = mocked_insert.call_args_list[0].args[1]
        assert "AnnotationOfAnswerSPARQL" in first_insert
        assert "SELECT DISTINCT ?answer" in first_insert
        assert "urn:qanary#question-1" in first_insert

    def test_without_annotations_stores_nothing(self):
        module, client = build_client()
        with mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "any question",
                                              "uri": "urn:qanary#question-1"}]), \
             mock.patch.object(module, "get_annotated_entities", return_value=[]), \
             mock.patch.object(module, "get_annotated_predicates", return_value=[P1]), \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert mocked_insert.call_count == 0
