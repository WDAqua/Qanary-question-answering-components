import importlib
from unittest import mock
from unittest import TestCase

from fastapi import FastAPI
from fastapi.testclient import TestClient


def build_client():
    from component import qe_sparqlexecuter
    importlib.reload(qe_sparqlexecuter)
    app = FastAPI()
    app.include_router(qe_sparqlexecuter.router)
    return qe_sparqlexecuter, TestClient(app)


REQUEST_DATA = {
    "values": {
        "urn:qanary#endpoint": "urn:qanary#test-endpoint",
        "urn:qanary#inGraph": "urn:qanary#test-inGraph",
        "urn:qanary#outGraph": "urn:qanary#test-outGraph",
    }
}

# a single binding holding a generated SPARQL query
SPARQL_BINDING = {
    "results": {"bindings": [{"sparql": {"value": "SELECT * WHERE { ?s ?p ?o } LIMIT 1"}}]}
}
EMPTY_BINDING = {"results": {"bindings": []}}


class TestQeSparqlExecuter(TestCase):

    def test_health_endpoint(self):
        _, client = build_client()
        response = client.get("/health")
        assert response.status_code == 200
        assert response.text == "alive"

    def test_execute_returns_result_dict(self):
        qe, _ = build_client()
        fake_sparql = mock.Mock()
        fake_sparql.query.return_value.convert.return_value = {"results": {"bindings": [{"x": 1}]}}
        with mock.patch.object(qe, "SPARQLWrapper", return_value=fake_sparql):
            result = qe.execute("SELECT * WHERE { ?s ?p ?o }", "http://endpoint.example/sparql")
        assert result == {"results": {"bindings": [{"x": 1}]}}

    def test_execute_returns_error_dict_on_exception(self):
        qe, _ = build_client()
        with mock.patch.object(qe, "SPARQLWrapper", side_effect=Exception("boom")):
            result = qe.execute("bad query")
        assert "error" in result
        assert "boom" in result["error"]

    def test_execute_logs_malformed_query(self):
        qe, _ = build_client()
        with mock.patch.object(qe, "SPARQLWrapper",
                               side_effect=Exception("MalformedQueryException: bad formed")):
            result = qe.execute("SELECT bad")
        assert "MalformedQueryException" in result["error"]

    def test_annotatequestion_executes_generated_sparql(self):
        qe, client = build_client()
        with mock.patch.object(qe, "get_text_question_in_graph",
                               return_value=[{"text": "q", "uri": "urn:q"}]), \
             mock.patch.object(qe, "query_triplestore", return_value=SPARQL_BINDING), \
             mock.patch.object(qe, "execute", return_value={"results": {"bindings": [{"answer": "42"}]}}) as mocked_execute, \
             mock.patch.object(qe, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        # the generated query was executed against the configured endpoint
        assert mocked_execute.called
        # an answer-JSON annotation was written
        assert mocked_insert.call_count == 1
        inserted_query = mocked_insert.call_args.args[1]
        assert "AnnotationOfAnswerJson" in inserted_query

    def test_annotatequestion_falls_back_when_no_sparql_found(self):
        """Regression: the no-SPARQL fallback must not raise (used to call
        json.loads() on a dict and 500)."""
        qe, client = build_client()
        with mock.patch.object(qe, "get_text_question_in_graph",
                               return_value=[{"text": "q", "uri": "urn:q"}]), \
             mock.patch.object(qe, "query_triplestore", return_value=EMPTY_BINDING), \
             mock.patch.object(qe, "insert_into_triplestore") as mocked_insert:

            response = client.post("/annotatequestion", json=REQUEST_DATA)

        assert response.status_code == 200
        # the dummy-answer annotation is still written
        assert mocked_insert.call_count == 1
        assert "AnnotationOfAnswerJson" in mocked_insert.call_args.args[1]
