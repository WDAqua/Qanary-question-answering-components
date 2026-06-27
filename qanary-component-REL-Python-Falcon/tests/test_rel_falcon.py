import importlib
from unittest import mock
from unittest import TestCase

from fastapi import FastAPI
from fastapi.testclient import TestClient


def build_client():
    """(re)import the component module and wrap its router in a throw-away app.

    The router is built at import time from the environment (see pytest.ini),
    so we reload to pick up the configured values and avoid run.py's
    Spring-Boot-Admin Registrator thread.
    """
    from component import rel_falcon
    importlib.reload(rel_falcon)
    app = FastAPI()
    app.include_router(rel_falcon.router)
    return rel_falcon, TestClient(app)


REQUEST_DATA = {
    "values": {
        "urn:qanary#endpoint": "urn:qanary#test-endpoint",
        "urn:qanary#inGraph": "urn:qanary#test-inGraph",
        "urn:qanary#outGraph": "urn:qanary#test-outGraph",
    }
}


class TestRelFalcon(TestCase):

    def test_health_endpoint(self):
        _, client = build_client()
        response = client.get("/health")
        assert response.status_code == 200
        assert response.text == "alive"

    def test_annotatequestion_inserts_relation_annotation(self):
        rel_falcon, client = build_client()

        falcon_response = mock.Mock()
        falcon_response.json.return_value = {
            "relations_dbpedia": [
                {"URI": "http://dbpedia.org/ontology/birthPlace"},
            ]
        }

        with mock.patch.object(rel_falcon, "get_text_question_in_graph",
                               return_value=[{"text": "Where was Einstein born?",
                                              "uri": "urn:qanary#question-1"}]) as mocked_get, \
             mock.patch.object(rel_falcon, "insert_into_triplestore") as mocked_insert, \
             mock.patch.object(rel_falcon.requests, "post", return_value=falcon_response) as mocked_post:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        # the request is echoed back unchanged
        assert response.status_code == 200
        assert response.json() == REQUEST_DATA

        # the question text was read from the triplestore
        assert mocked_get.called
        # FALCON was queried with the question text
        assert mocked_post.called

        # exactly one relation annotation was written, carrying the FALCON URI
        assert mocked_insert.call_count == 1
        inserted_query = mocked_insert.call_args.args[1]
        assert "AnnotationOfRelation" in inserted_query
        assert "http://dbpedia.org/ontology/birthPlace" in inserted_query

    def test_annotatequestion_handles_multiple_relations(self):
        rel_falcon, client = build_client()

        falcon_response = mock.Mock()
        falcon_response.json.return_value = {
            "relations_dbpedia": [
                {"URI": "http://dbpedia.org/ontology/birthPlace"},
                {"URI": "http://dbpedia.org/ontology/spouse"},
            ]
        }

        with mock.patch.object(rel_falcon, "get_text_question_in_graph",
                               return_value=[{"text": "q", "uri": "urn:q"}]), \
             mock.patch.object(rel_falcon, "insert_into_triplestore") as mocked_insert, \
             mock.patch.object(rel_falcon.requests, "post", return_value=falcon_response):

            client.post("/annotatequestion", json=REQUEST_DATA)

        # one INSERT per relation returned by FALCON
        assert mocked_insert.call_count == 2
