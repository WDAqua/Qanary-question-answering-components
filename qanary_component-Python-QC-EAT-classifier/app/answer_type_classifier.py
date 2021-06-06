import os
import requests
import json
import logging
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)
answer_type_classifier = Blueprint('answer_type_classifier', __name__, template_folder='templates')

CLASSIFICATION_ENDPOINT = os.environ['CLASSIFICATION_ENDPOINT']
SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']


@answer_type_classifier.route("/annotatequestion", methods=['POST'])
def qanary_service():
    """the POST endpoint required for a Qanary service"""
    
    triplestore_endpoint = request.json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request.json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request.json["values"]["urn:qanary#outGraph"]

    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    text = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, graph=triplestore_ingraph)[0]['text']
    
    logging.info(f'Question Text: {text}')
    
    data = {'questions': [text]}  # creating params dict for the service

    json_response = requests.post(CLASSIFICATION_ENDPOINT, data=data)  # making a request to the service
    predicted_answer_type = json.loads(json_response.text)['predictions'][0]

    # building SPARQL query
    SPARQLquery = """
                    PREFIX qa: <http://www.wdaqua.eu/qa#>
                    PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
                    PREFIX dbo: <http://dbpedia.org/ontology/>

                    INSERT {{
                    GRAPH <{uuid}> {{
                        ?a a qa:AnnotationOfAnswerTypeClassifier .
                        ?a qa:hasAnswerType dbo:{answer_type} .

                        ?a oa:annotatedBy <urn:qanary:{app_name}> .
                        ?a oa:annotatedAt ?time .
                        }}
                    }}
                    WHERE {{
                        BIND (IRI(str(RAND())) AS ?a) .
                        BIND (now() as ?time) 
                    }}
                """.format(
                    uuid=triplestore_ingraph,
                    answer_type=predicted_answer_type,
                    app_name="{0}:Python".format(SERVICE_NAME_COMPONENT)
                )
    
    logging.info(f'SPARQL: {SPARQLquery}')
    # inserting new data to the triplestore
    insert_into_triplestore(triplestore_endpoint, SPARQLquery)

    return jsonify(request.get_json())


@answer_type_classifier.route("/", methods=['GET'])
def index():
    """an examplary GET endpoint returning "hello world (String)"""
    logging.info("host_url: %s" % (request.host_url,))
    return "Hi! \n This is Answer Type Classification component, based on DBpedia Ontology."
