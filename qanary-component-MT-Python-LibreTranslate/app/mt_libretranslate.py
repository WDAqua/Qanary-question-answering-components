from langdetect import detect
import logging
import os
import requests
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore


logging.basicConfig(format="%(asctime)s - %(message)s", level=logging.INFO)

mt_libretranslate_bp = Blueprint("mt_libretranslate_bp", __name__, template_folder="templates")

SERVICE_NAME_COMPONENT = os.environ["SERVICE_NAME_COMPONENT"]


target_lang = 'en'
lt_url = "http://localhost:5000/translate"



@mt_libretranslate_bp.route("/annotatequestion", methods=["POST"])
def qanary_service():
    """the POST endpoint required for a Qanary service"""

    triplestore_endpoint = request.json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request.json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request.json["values"]["urn:qanary#outGraph"]
    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % \
                 (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    text = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, 
                                      graph=triplestore_ingraph)[0]["text"]
    question_uri = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint,
                                              graph=triplestore_ingraph)[0]["uri"]
    logging.info(f"Question text: {text}")

    #lang, prob = langid.classify(text)
    lang = detect(text)
    logging.info(f"source language: {lang}")

    ## TODO: MAIN FUNCTIONALITY
    result = translate_input(text, lang)

    # building SPARQL query TODO: verify this annotation AnnotationOfQuestionTranslation ??
    SPARQLquery = """
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        INSERT {{
        GRAPH <{uuid}> {{
            ?a a qa:AnnotationOfQuestionTranslation ;
                oa:hasTarget <{qanary_question_uri}> ; 
                oa:hasBody "{translation_result}"@en ;
                oa:annotatedBy <urn:qanary:{app_name}> ;
                oa:annotatedAt ?time .
            ?b a qa:AnnotationOfQuestionLanguage ;
                oa:hasTarget <{qanary_question_uri}> ;
                oa:hasBody "{src_lang}"^^xsd:string ;
                oa:annotatedBy <urn:qanary:{app_name}> ;
                oa:annotatedAt ?time .
            }}
        }}
        WHERE {{
            BIND (IRI(str(RAND())) AS ?a) .
            BIND (IRI(str(RAND())) AS ?b) .
            BIND (now() as ?time) 
        }}
    """.format(
        uuid=triplestore_ingraph,
        qanary_question_uri=question_uri,
        translation_result=result,
        src_lang=lang,
        app_name=SERVICE_NAME_COMPONENT
    )
    
    logging.info(f'SPARQL: {SPARQLquery}')
    # inserting new data to the triplestore
    insert_into_triplestore(triplestore_endpoint, SPARQLquery)

    return jsonify(request.get_json())


@mt_libretranslate_bp.route("/", methods=["GET"])
def index():
    """examplary GET endpoint"""

    logging.info("host_url: %s" % (request.host_url))
    return "Python MT LibreTranslate Qanary component"


def translate_input(text, source_lang):

    req_json = {
        'q': text,
        'source': source_lang,
        'target': target_lang
    }
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    response = requests.request("POST", lt_url, headers=headers, data=req_json)
    logging.info(f"got response json: {response.json()}")
    translation = response.json()['translatedText']
    return translation


def test_connection():
    logging.info(f"testing connection to {lt_url}")
    try:
        t = translate_input("eingabe zum testen", "de")
        logging.info(f"got translation: {t}")
        assert len(t) > 0
        return True
    except Exception as e: 
        logging.info(f"test failed with {e}")
        return False
