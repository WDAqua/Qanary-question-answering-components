import importlib
from unittest import mock
from unittest import TestCase

from fastapi import FastAPI
from fastapi.testclient import TestClient


def build_client():
    """(re)import the component module and wrap its router in a throw-away app.

    The router and configuration are built at import time from the environment
    (see pytest.ini), so we reload to pick up the configured values and avoid
    run.py's Spring-Boot-Admin Registrator thread.
    """
    from component import rd_predicate_identifier
    importlib.reload(rd_predicate_identifier)
    app = FastAPI()
    app.include_router(rd_predicate_identifier.router)
    return rd_predicate_identifier, TestClient(app)


REQUEST_DATA = {
    "values": {
        "urn:qanary#endpoint": "urn:qanary#test-endpoint",
        "urn:qanary#inGraph": "urn:qanary#test-inGraph",
        "urn:qanary#outGraph": "urn:qanary#test-outGraph",
    }
}

# a realistic slice of the predicates DBpedia exposes for dbr:Germany
GERMANY_CANDIDATES = [
    ("http://dbpedia.org/ontology/capital", None),
    ("http://dbpedia.org/property/capital", None),
    ("http://dbpedia.org/ontology/currency", None),
    ("http://dbpedia.org/ontology/populationTotal", None),
    ("http://dbpedia.org/ontology/largestCity", None),
    ("http://dbpedia.org/ontology/leaderName", None),
    ("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", None),
    ("http://www.w3.org/2000/01/rdf-schema#label", None),
]


class TestPurePredicateSelection(TestCase):
    """Deterministic, network-free tests of the ranking logic."""

    def test_tokenize_splits_camel_case_and_drops_stopwords(self):
        module, _ = build_client()
        assert module.tokenize("birthPlace") == ["birth", "place"]
        # "of", "the" are stop words; "capital"/"Germany" survive
        assert module.tokenize("What is the capital of Germany") == ["capital", "germany"]

    def test_local_name(self):
        module, _ = build_client()
        assert module.local_name("http://dbpedia.org/ontology/capital") == "capital"
        assert module.local_name("http://www.w3.org/2000/01/rdf-schema#label") == "label"

    def test_capital_predicate_scores_perfectly(self):
        module, _ = build_client()
        question_tokens = module.tokenize("What is the capital of Germany")
        assert module.match_score("capital", question_tokens) == 1.0

    def test_selects_capital_for_capital_question(self):
        module, _ = build_client()
        predicate, score = module.select_best_predicate(
            "What is the capital of Germany", GERMANY_CANDIDATES)
        # the ontology predicate wins over the free-text dbp:capital on the tie
        assert predicate == "http://dbpedia.org/ontology/capital"
        assert score >= 0.5

    def test_selects_currency_for_currency_question(self):
        module, _ = build_client()
        predicate, _ = module.select_best_predicate(
            "What currency does Germany use", GERMANY_CANDIDATES)
        assert predicate == "http://dbpedia.org/ontology/currency"

    def test_prefers_rdfs_label_over_local_name(self):
        module, _ = build_client()
        # a Wikidata-style opaque predicate whose meaning is only in its label
        candidates = [("http://www.wikidata.org/prop/direct/P36", "capital")]
        predicate, score = module.select_best_predicate(
            "What is the capital of Germany", candidates)
        assert predicate == "http://www.wikidata.org/prop/direct/P36"
        assert score == 1.0

    def test_no_candidates_returns_none(self):
        module, _ = build_client()
        predicate, score = module.select_best_predicate("anything", [])
        assert predicate is None
        assert score == 0.0

    def test_unrelated_question_below_threshold_returns_none(self):
        module, _ = build_client()
        predicate, _ = module.select_best_predicate(
            "xyzzy plugh frobnicate", GERMANY_CANDIDATES)
        assert predicate is None


class TestFetchCandidatePredicates(TestCase):

    def test_dedupes_predicates_and_keeps_english_label(self):
        module, _ = build_client()
        bindings = [
            {"p": {"value": "http://dbpedia.org/ontology/capital"}},
            {"p": {"value": "http://dbpedia.org/ontology/capital"},
             "pLabel": {"value": "capital"}},
            {"p": {"value": "http://dbpedia.org/ontology/currency"},
             "pLabel": {"value": "currency"}},
        ]
        with mock.patch.object(module, "run_select_query", return_value=bindings):
            candidates = dict(module.fetch_candidate_predicates(
                "https://dbpedia.org/sparql", "http://dbpedia.org/resource/Germany"))
        assert candidates["http://dbpedia.org/ontology/capital"] == "capital"
        assert candidates["http://dbpedia.org/ontology/currency"] == "currency"

    def test_falls_back_to_plain_query_when_labelled_query_fails(self):
        module, _ = build_client()
        plain_bindings = [{"p": {"value": "http://dbpedia.org/ontology/capital"}}]
        with mock.patch.object(module, "run_select_query",
                               side_effect=[Exception("OPTIONAL rejected"), plain_bindings]) as mocked:
            candidates = module.fetch_candidate_predicates(
                "https://dbpedia.org/sparql", "http://dbpedia.org/resource/Germany")
        assert mocked.call_count == 2
        assert candidates == [("http://dbpedia.org/ontology/capital", None)]

    def test_resolves_wikidata_property_labels(self):
        module, _ = build_client()
        # wdt: predicates have no rdfs:label of their own, so the first query
        # yields an unlabelled predicate and a second (wikibase:directClaim)
        # query supplies the label
        predicate_bindings = [{"p": {"value": "http://www.wikidata.org/prop/direct/P36"}}]
        label_bindings = [{"p": {"value": "http://www.wikidata.org/prop/direct/P36"},
                           "pLabel": {"value": "capital"}}]
        with mock.patch.object(module, "run_select_query",
                               side_effect=[predicate_bindings, label_bindings]) as mocked:
            candidates = module.fetch_candidate_predicates(
                "https://query.wikidata.org/sparql",
                "http://www.wikidata.org/entity/Q183")
        assert mocked.call_count == 2
        assert dict(candidates)["http://www.wikidata.org/prop/direct/P36"] == "capital"

    def test_wikidata_label_resolution_failure_is_tolerated(self):
        module, _ = build_client()
        predicate_bindings = [{"p": {"value": "http://www.wikidata.org/prop/direct/P36"}}]
        with mock.patch.object(module, "run_select_query",
                               side_effect=[predicate_bindings, Exception("no wikibase:")]):
            candidates = module.fetch_candidate_predicates(
                "https://query.wikidata.org/sparql",
                "http://www.wikidata.org/entity/Q183")
        # the opaque predicate is still returned, just without a resolved label
        assert candidates == [("http://www.wikidata.org/prop/direct/P36", None)]


class TestAnnotateQuestion(TestCase):

    def test_health_endpoint(self):
        _, client = build_client()
        response = client.get("/health")
        assert response.status_code == 200
        assert response.text == "alive"

    def test_annotatequestion_inserts_capital_relation(self):
        module, client = build_client()

        with mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "What is the capital of Germany",
                                              "uri": "urn:qanary#question-1"}]), \
             mock.patch.object(module, "get_identified_resources",
                               return_value=["http://dbpedia.org/resource/Germany"]), \
             mock.patch.object(module, "fetch_candidate_predicates",
                               return_value=GERMANY_CANDIDATES), \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert response.json() == REQUEST_DATA

        # exactly one relation annotation, carrying the ontology capital predicate
        assert mocked_insert.call_count == 1
        inserted_query = mocked_insert.call_args.args[1]
        assert "AnnotationOfRelation" in inserted_query
        assert "http://dbpedia.org/ontology/capital" in inserted_query
        # it targets the question and records a score
        assert "urn:qanary#question-1" in inserted_query
        assert "qa:score" in inserted_query

    def test_annotatequestion_without_resource_writes_nothing(self):
        module, client = build_client()

        with mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "What is the capital of Germany",
                                              "uri": "urn:qanary#question-1"}]), \
             mock.patch.object(module, "get_identified_resources", return_value=[]), \
             mock.patch.object(module, "fetch_candidate_predicates") as mocked_fetch, \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        # no resource -> the knowledge graph is never queried and nothing is stored
        assert mocked_fetch.call_count == 0
        assert mocked_insert.call_count == 0

    def test_annotatequestion_no_matching_predicate_writes_nothing(self):
        module, client = build_client()

        with mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "xyzzy plugh frobnicate",
                                              "uri": "urn:qanary#question-1"}]), \
             mock.patch.object(module, "get_identified_resources",
                               return_value=["http://dbpedia.org/resource/Germany"]), \
             mock.patch.object(module, "fetch_candidate_predicates",
                               return_value=GERMANY_CANDIDATES), \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert mocked_insert.call_count == 0
