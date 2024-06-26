from langdetect import detect
import logging
import os
import requests
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore


logging.basicConfig(format="%(asctime)s - %(message)s", level=logging.INFO)

mt_libretranslate_bp = Blueprint("mt_libretranslate_bp", __name__, template_folder="templates")

SERVICE_NAME_COMPONENT = os.environ["SERVICE_NAME_COMPONENT"]

SOURCE_LANG = os.environ["SOURCE_LANGUAGE"]
#TARGET_LANG = os.environ["TARGET_LANGUAGE"]
TARGET_LANG = "en" # currently only supports English
TRANSLATE_ENDPOINT = os.environ["TRANSLATE_ENDPOINT"]
LANGUAGES_ENDPOINT = os.environ["LANGUAGES_ENDPOINT"]


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

    if SOURCE_LANG != None and len(SOURCE_LANG.strip()) > 0:
        lang = SOURCE_LANG
        logging.info("Using custom SOURCE_LANGUAGE")
    else:
        lang = detect(text)
        logging.info("No SOURCE_LANGUAGE specified, using langdetect!")
    logging.info(f"source language: {lang}")

    #lang, prob = langid.classify(text)
    lang = detect(text)
    logging.info(f"source language: {lang}")

    ## TODO: MAIN FUNCTIONALITY
    result, _ = translate_input(text, lang, TARGET_LANG)

    # building SPARQL query TODO: verify this annotation AnnotationOfQuestionTranslation ??
    SPARQLqueryAnnotationOfQuestionTranslation = """
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

        INSERT {{
        GRAPH <{uuid}> {{
            ?a a qa:AnnotationOfQuestionTranslation ;
                oa:hasTarget <{qanary_question_uri}> ;
                oa:hasBody "{translation_result}"@{target_lang} ;
                oa:annotatedBy <urn:qanary:{app_name}> ;
                oa:annotatedAt ?time .

            }}
        }}
        WHERE {{
            BIND (IRI(str(RAND())) AS ?a) .
            BIND (now() as ?time)
        }}
    """.format(
        uuid=triplestore_ingraph,
        qanary_question_uri=question_uri,
        translation_result=result.replace("\"", "\\\""), #keep quotation marks that are part of the translation
        target_lang=TARGET_LANG,
        app_name=SERVICE_NAME_COMPONENT
    )

    SPARQLqueryAnnotationOfQuestionLanguage = """
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

        INSERT {{
        GRAPH <{uuid}> {{
            ?b a qa:AnnotationOfQuestionLanguage ;
                oa:hasTarget <{qanary_question_uri}> ;
                oa:hasBody "{src_lang}"^^xsd:string ;
                oa:annotatedBy <urn:qanary:{app_name}> ;
                oa:annotatedAt ?time .
            }}
        }}
        WHERE {{
            BIND (IRI(str(RAND())) AS ?b) .
            BIND (now() as ?time)
        }}
    """.format(
        uuid=triplestore_ingraph,
        qanary_question_uri=question_uri,
        src_lang=lang,
        app_name=SERVICE_NAME_COMPONENT
    )

    logging.info(f'SPARQL: {SPARQLqueryAnnotationOfQuestionTranslation}')
    logging.info(f'SPARQL: {SPARQLqueryAnnotationOfQuestionLanguage}')
    # inserting new data to the triplestore
    insert_into_triplestore(triplestore_endpoint, SPARQLqueryAnnotationOfQuestionTranslation)
    insert_into_triplestore(triplestore_endpoint, SPARQLqueryAnnotationOfQuestionLanguage)

    return jsonify(request.get_json())


@mt_libretranslate_bp.route("/", methods=["GET"])
def index():
    """examplary GET endpoint"""

    logging.info("host_url: %s" % (request.host_url))
    return "Python MT LibreTranslate Qanary component"


def translate_input(text, source_lang, target_lang):

    req_json = {
        'q': text,
        'source': source_lang,
        'target': target_lang
    }
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    response = requests.request("POST", TRANSLATE_ENDPOINT, headers=headers, data=req_json)
    logging.info(f"got response json: {response.json()}")
    translation = response.json().get('translatedText')
    error = response.json().get('error')
    return translation, error


def check_connection():
    logging.info(f"checking connection to {TRANSLATE_ENDPOINT}")
    error = "(No error message available)" #empty error message
    success = "The test translation was successful"
    try:
        # TODO: test with supported language? 
        t, error = translate_input("eingabe zum testen", "de", "en")
        logging.info(f"got translation: {t}")
        assert len(t) > 0
        return True, success
    except Exception:
        logging.info(f"test failed with {error}")
        return False, error


def get_languages():
    languages = requests.request("GET", LANGUAGES_ENDPOINT).json()
    valid_sources = [(f"{language['name']} ({language['code']})") for language in languages if 'en' in language['targets'] and 'en' not in language['code']]
    return valid_sources
