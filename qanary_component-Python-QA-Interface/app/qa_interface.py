import os
import requests
import json
import logging
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore, select_from_triplestore

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)
qa_interface = Blueprint('qa_interface', __name__, template_folder='templates')

QA_SYSTEM_NAME = os.environ['QA_SYSTEM_NAME']
with open(os.environ['QA_SYSTEM_CONFIG']) as f:
    QA_SYSTEM_CONFIG = json.load(f)[QA_SYSTEM_NAME]
    
QA_SYSTEM_URL = QA_SYSTEM_CONFIG['QA_SYSTEM_URL']
QA_SYSTEM_PARAMS = QA_SYSTEM_CONFIG['QA_SYSTEM_PARAMS']
SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']


@qa_interface.route("/annotatequestion", methods=['POST'])
def qanary_service():
    """the POST endpoint required for a Qanary service"""
    
    triplestore_endpoint = request.json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request.json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request.json["values"]["urn:qanary#outGraph"]
    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    # check if translation was involved
    SPARQLquery = """
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>

        SELECT ?val
        FROM <{uuid}> {{
            ?s a qa:AnnotationOfQuestionLanguage ;
                qa:translationResult ?val .
        }}
    """.format(uuid=triplestore_ingraph)

    translation = select_from_triplestore(triplestore_endpoint, SPARQLquery)
    logging.info(f" {translation}")

    if 'results' in translation.keys() and len(translation['results']['bindings']) > 0:
        text = list(translation['results']['bindings'][0].values())[0]['value']
    else:
        text = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, graph=triplestore_ingraph)[0]['text']

    logging.info(f'Question Text: {text}')
    
    QA_SYSTEM_PARAMS['question'] = text

    json_response = requests.get(QA_SYSTEM_URL.format(**QA_SYSTEM_PARAMS)).json()  # making a request to a service
    if len(json_response['answer']) > 0 and 'http' in json_response['answer'][0]:
        results = ', '.join('<' + uri + '>' for uri in json_response['answer'])
    elif len(json_response['answer']) > 0 and 'http' not in json_response['answer'][0]:
        results = ', '.join('"' + uri + '"' for uri in json_response['answer'])
    else:
        results = '"No information available"'

    # building SPARQL query
    SPARQLquery = """
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        
        INSERT {{
        GRAPH <{uuid}> {{
                ?a a qa:AnnotationOfQaInterface ;
                    oa:annotatedBy <urn:qanary:{app_name}:{qa_system_name}> ;
                    oa:annotatedAt ?time ;
                    oa:answerResult {results} .
            }}
        }}
        WHERE {{
            BIND (IRI(str(RAND())) AS ?a) .
            BIND (now() as ?time) 
        }}
    """.format(
        uuid=triplestore_ingraph,
        qa_system_name=QA_SYSTEM_NAME,
        qa_system_url=QA_SYSTEM_URL.format(**QA_SYSTEM_PARAMS),
        results=results,
        app_name="{0}:Python".format(SERVICE_NAME_COMPONENT)
    )
    
    logging.info(f'SPARQL: {SPARQLquery}')
    # inserting new data to the triplestore
    insert_into_triplestore(triplestore_endpoint, SPARQLquery)

    return jsonify(request.get_json())


@qa_interface.route("/", methods=['GET'])
def index():
    """an examplary GET endpoint returning "hello world (String)"""

    logging.info("host_url: %s" % (request.host_url,))
    return "Hi! \n This is Python QA Interface component"