"""A minimal offline SPARQL-over-HTTP server for the E2E tests.

Backed by rdflib, it stands in for the two external systems of a Qanary
pipeline and binds to 127.0.0.1 only (no external network):

* ``POST /sparql``     -- the Qanary triplestore (an ``rdflib.Dataset`` with
  named graphs; accepts SPARQL ``query`` and ``update`` requests, as sent by
  ``qanary_helpers`` / SPARQLWrapper)
* ``POST /kg/sparql``  -- the knowledge-graph endpoint (an ``rdflib.Graph``;
  query only), used by the component to validate candidate graph patterns
* ``GET /questions/<id>/raw`` -- the textual question, as served by the Qanary
  pipeline for a question URI (``get_text_question_in_graph`` fetches this)
"""
import threading
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from rdflib import Dataset, Graph

EMPTY_RESULT = b'{"head": {"vars": []}, "results": {"bindings": []}}'


class OfflineSparqlServer:
    """Context manager running the server on an ephemeral local port."""

    def __init__(self, kg_turtle_path: str):
        self.dataset = Dataset()           # the Qanary triplestore
        self.knowledge_graph = Graph()     # the provided knowledge graph
        self.knowledge_graph.parse(kg_turtle_path, format="turtle")
        self.questions = {}                # question id -> question text
        self._server = None
        self._thread = None

    # ------------------------------------------------------------------ #
    def __enter__(self):
        outer = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):  # keep pytest output clean
                pass

            def _reply(self, payload: bytes,
                       content_type="application/sparql-results+json"):
                self.send_response(200)
                self.send_header("Content-Type", content_type)
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def do_GET(self):
                parts = self.path.strip("/").split("/")
                if len(parts) == 3 and parts[0] == "questions" and parts[2] == "raw":
                    text = outer.questions.get(parts[1], "")
                    self._reply(text.encode("utf-8"), "text/plain; charset=utf-8")
                else:
                    self.send_error(404)

            def do_POST(self):
                length = int(self.headers.get("Content-Length", 0))
                fields = urllib.parse.parse_qs(self.rfile.read(length).decode("utf-8"))
                query = fields.get("query", [None])[0]
                update = fields.get("update", [None])[0]
                if self.path.startswith("/kg/"):
                    if query is None:
                        self.send_error(400, "query required")
                        return
                    result = outer.knowledge_graph.query(query)
                    self._reply(result.serialize(format="json"))
                elif self.path.startswith("/sparql"):
                    if update is not None:
                        outer.dataset.update(update)
                        self._reply(EMPTY_RESULT)
                    elif query is not None:
                        result = outer.dataset.query(query)
                        self._reply(result.serialize(format="json"))
                    else:
                        self.send_error(400, "query or update required")
                else:
                    self.send_error(404)

        self._server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self._thread = threading.Thread(target=self._server.serve_forever,
                                        daemon=True)
        self._thread.start()
        return self

    def __exit__(self, *exc):
        self._server.shutdown()
        self._server.server_close()
        self._thread.join(timeout=5)
        return False

    # ------------------------------------------------------------------ #
    @property
    def base_url(self) -> str:
        host, port = self._server.server_address
        return f"http://{host}:{port}"

    @property
    def triplestore_endpoint(self) -> str:
        return f"{self.base_url}/sparql"

    @property
    def knowledge_graph_endpoint(self) -> str:
        return f"{self.base_url}/kg/sparql"

    def question_uri(self, question_id: str) -> str:
        return f"{self.base_url}/questions/{question_id}"

    def add_question(self, question_id: str, text: str):
        self.questions[question_id] = text
