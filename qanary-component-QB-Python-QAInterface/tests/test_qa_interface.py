import importlib
from unittest import mock
from unittest import TestCase


def build_client():
    """Use the Flask app the component assembles in component/__init__.py
    (blueprint + /health + /about), and return the submodule so its
    qanary_helpers calls can be patched."""
    from component import app
    module = importlib.import_module("component.qa_interface")
    return module, app.test_client()


REQUEST_DATA = {
    "values": {
        "urn:qanary#endpoint": "urn:qanary#test-endpoint",
        "urn:qanary#inGraph": "urn:qanary#test-inGraph",
        "urn:qanary#outGraph": "urn:qanary#test-outGraph",
    }
}

NO_TRANSLATION = {"results": {"bindings": []}}
WITH_TRANSLATION = {"results": {"bindings": [{"val": {"value": "translated question"}}]}}


def _answer(uris):
    resp = mock.Mock()
    resp.json.return_value = {"answer": uris}
    return resp


class TestQaInterface(TestCase):

    def test_index_endpoint(self):
        _, client = build_client()
        response = client.get("/")
        assert response.status_code == 200
        assert b"QA Interface" in response.data

    def test_health_endpoint(self):
        _, client = build_client()
        response = client.get("/health")
        assert response.status_code == 200
        assert b"alive" in response.data

    def test_about_endpoint(self):
        _, client = build_client()
        response = client.get("/about")
        assert response.status_code == 200

    def test_annotatequestion_inserts_answer_sparql(self):
        module, client = build_client()
        with mock.patch.object(module, "select_from_triplestore", return_value=NO_TRANSLATION), \
             mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "Where is Berlin?", "uri": "urn:q"}]), \
             mock.patch.object(module.requests, "get",
                               return_value=_answer(["http://dbpedia.org/resource/Berlin"])), \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert mocked_insert.call_count == 1
        query = mocked_insert.call_args.args[1]
        assert "AnnotationOfAnswerSPARQL" in query
        # URI answers are wrapped in angle brackets
        assert "<http://dbpedia.org/resource/Berlin>" in query

    def test_annotatequestion_uses_existing_translation(self):
        module, client = build_client()
        with mock.patch.object(module, "select_from_triplestore", return_value=WITH_TRANSLATION), \
             mock.patch.object(module, "get_text_question_in_graph") as mocked_get_text, \
             mock.patch.object(module.requests, "get", return_value=_answer(["literal answer"])), \
             mock.patch.object(module, "insert_into_triplestore"):

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        # when a translation exists, the original question text is NOT fetched
        mocked_get_text.assert_not_called()

    def test_annotatequestion_handles_no_answer(self):
        module, client = build_client()
        with mock.patch.object(module, "select_from_triplestore", return_value=NO_TRANSLATION), \
             mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "q", "uri": "urn:q"}]), \
             mock.patch.object(module.requests, "get", return_value=_answer([])), \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert "No information available" in mocked_insert.call_args.args[1]
