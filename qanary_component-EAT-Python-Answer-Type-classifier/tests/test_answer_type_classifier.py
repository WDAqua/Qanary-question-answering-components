import importlib
from unittest import mock
from unittest import TestCase


def build_client():
    """Use the Flask app assembled in app/__init__.py (blueprint + /health),
    and return the submodule so its qanary_helpers calls can be patched."""
    from app import app
    module = importlib.import_module("app.answer_type_classifier")
    return module, app.test_client()


REQUEST_DATA = {
    "values": {
        "urn:qanary#endpoint": "urn:qanary#test-endpoint",
        "urn:qanary#inGraph": "urn:qanary#test-inGraph",
        "urn:qanary#outGraph": "urn:qanary#test-outGraph",
    }
}


def _classifier_response(prediction):
    resp = mock.Mock()
    resp.text = '{"predictions": ["%s"]}' % prediction
    return resp


class TestAnswerTypeClassifier(TestCase):

    def test_index_endpoint(self):
        _, client = build_client()
        response = client.get("/")
        assert response.status_code == 200
        assert b"Answer Type Classification" in response.data

    def test_health_endpoint(self):
        _, client = build_client()
        response = client.get("/health")
        assert response.status_code == 200
        assert b"alive" in response.data

    def test_annotatequestion_inserts_answer_type(self):
        module, client = build_client()
        with mock.patch.object(module, "get_text_question_in_graph",
                               return_value=[{"text": "Who is Einstein?", "uri": "urn:q"}]), \
             mock.patch.object(module.requests, "post",
                               return_value=_classifier_response("Person")) as mocked_post, \
             mock.patch.object(module, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        assert mocked_post.called
        assert mocked_insert.call_count == 1
        query = mocked_insert.call_args.args[1]
        # the (now well-formed) annotation carries the predicted DBpedia answer type
        assert "AnnotationOfQuestionAnswerType" in query
        assert "qa:qa:" not in query
        assert "dbo:Person" in query
