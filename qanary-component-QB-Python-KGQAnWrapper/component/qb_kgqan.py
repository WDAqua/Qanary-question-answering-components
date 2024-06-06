import requests
import re
import logging
import os
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore


logging.basicConfig(format="%(asctime)s - %(message)s", level=logging.DEBUG)

qb_kgqan_bp = Blueprint("qb_kgqan_bp", __name__, template_folder="templates")

SERVICE_NAME_COMPONENT = os.environ["SERVICE_NAME_COMPONENT"]
KGQAN_ENDPOINT = os.environ["KGQAN_ENDPOINT"]
KGQAN_KNOWLEDGEGRAPH = os.environ["KGQAN_KNOWLEDGEGRAPH"]
KGQAN_MAX_ANSWERS = os.environ["KGQAN_MAX_ANSWERS"]


@qb_kgqan_bp.route("/annotatequestion", methods=["POST"])
def qanary_service():
    """the POST endpoint required for a Qanary service"""

    triplestore_endpoint = request.json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request.json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request.json["values"]["urn:qanary#outGraph"]
    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % \
                 (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    quetsion_text = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint,
                                      graph=triplestore_ingraph)[0]["text"]
    question_uri = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint,
                                              graph=triplestore_ingraph)[0]["uri"]
    logging.info(f"Question text: {quetsion_text}")


    ## MAIN FUNCTIONALITY
    candidate_list = call_kgqan_endpoint(quetsion_text)

    # create sparql insert queries 
    for candidate in candidate_list:
        # query candidate
        sparql_AnnotationOfAnswerSPARQL = """
            PREFIX dbr: <http://dbpedia.org/resource/>
            PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
            PREFIX qa: <http://www.wdaqua.eu/qa#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

            INSERT {{
                GRAPH <{graph}>  {{
                    ?newAnnotation rdf:type qa:AnnotationOfAnswerSPARQL .
                    ?newAnnotation oa:hasTarget <{target_question}> .
                    ?newAnnotation oa:hasBody "{answer_sparql}" .
                    ?newAnnotation qa:score "{confidence}"^^xsd:float .
                    ?newAnnotation qa:index "{index}"^^xsd:integer .
                    ?newAnnotation oa:annotatedAt ?time .
                    ?newAnnotation oa:annotatedBy <urn:qanary:{component_name}> .
                }}
            }}
            WHERE {{
                BIND (IRI(CONCAT("urn:qanary:annotation:answer:sparql:", STR(RAND()))) AS ?newAnnotation) .
                BIND (now() as ?time) .
            }}
        """.format(
            graph = triplestore_ingraph,
            target_question = question_uri,
            answer_sparql = candidate.get("sparql"),
            confidence = candidate.get("confidence"),
            index = candidate.get("index"),
            component_name = SERVICE_NAME_COMPONENT
        )
        logging.debug(f"SPARQL for query candidates:\n{sparql_AnnotationOfAnswerSPARQL}")
        insert_into_triplestore(triplestore_endpoint, sparql_AnnotationOfAnswerSPARQL)

        # TODO: answer json

        # TODO: answer data type
#        sparql_AnnotationOfAnswerDataType = """
#            PREFIX dbr: <http://dbpedia.org/resource/>
#            PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
#            PREFIX qa: <http://www.wdaqua.eu/qa#>
#            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
#            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
#
#            INSERT {
#                GRAPH <{graph}>  {
#                    ?newAnnotation rdf:type qa:AnnotationOfAnswerDataType .
#                    ?newAnnotation oa:hasTarget <{target_question}> .
#                    ?newAnnotation qa:answerDataType <{answer_datatype}> .
#                    ?newAnnotation oa:annotatedAt ?time .
#                    ?newAnnotation oa:annotatedBy <urn:qanary:{component_name}> .
#                }
#            }
#            WHERE {
#                BIND (IRI(CONCAT("urn:qanary:annotation:answer:sparql:", STR(RAND()))) AS ?newAnnotation) .
#                BIND (now() as ?time) .
#            }
#        """.format(
#            graph = triplestore_ingraph,
#            target_question = question_uri,
#            answer_datatype = ?
#            component_name = SERVICE_NAME_COMPONENT
#        )

    return jsonify(request.get_json())


def call_kgqan_endpoint(question_text: str):
    json = {
        'question': question_text,
        'knowledge_graph': KGQAN_KNOWLEDGEGRAPH,
        'max_answers': int(KGQAN_MAX_ANSWERS)
    }
    headers = {
        'Content-Type': 'application/json'
    }
    response = requests.request("POST", KGQAN_ENDPOINT, json=json, headers=headers)

    if response.status_code != 200:
        raise RuntimeError(f"Could not fetch answer from KGQAn server: {response.status_code}:\n{response.text}")

    response_json = response.json()
    logging.debug(f"got response json: {response_json}")

    candidate_list = []
    results = response_json#[0]
    for index, result in enumerate(results):
        logging.debug(f"candidate #{index}: {result}")
        candidate = {
            'sparql': clean_sparql_for_insert_query(result.get("sparql")),
            'values': result.get("values"),
            'confidence': result.get("score"),
            'index': index
        }
        candidate_list.append(candidate)

    return candidate_list


def clean_sparql_for_insert_query(sparql: str):

    cleaned_sparql = re.sub(r"OPTIONAL\W*\{(.*?)\}", "", sparql.replace("?type", ""))

    return cleaned_sparql

#def check_connection():
#    logging.info(f"checking connection to {KGQAN_ENDPOINT}")
#    error = "(No error message available)" #empty error message
#    success = "The test translation was successful"
#    try:
#        # TODO: test with supported language? 
#        r, error = call_kgqan_server()
#        assert len(r) > 0
#        return True, success
#    except Exception:
#        logging.info(f"test failed with {error}")
#        return False, error


@qb_kgqan_bp.route("/", methods=["GET"])
def index():
    """examplary GET endpoint"""

    logging.info("host_url: %s" % (request.host_url))
    return "Python QB KGQAn Qanary component"
