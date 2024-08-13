import langid
import logging
import os
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore
from utils.model_utils import load_models_and_tokenizers

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)
mt_helsinki_nlp_bp = Blueprint('mt_helsinki_nlp_bp', __name__, template_folder='templates')

SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']
SOURCE_LANG_DEFAULT = os.environ["SOURCE_LANGUAGE_DEFAULT"]
TARGET_LANG_DEFAULT = os.environ["TARGET_LANGUAGE_DEFAULT"]
SUPPORTED_LANGS = {
#   source: targets
    'en': ['de', 'fr', 'ru', 'es'],
    'de': ['en', 'fr', 'es'],
    'fr': ['en', 'de', 'ru', 'es'],
    'ru': ['en', 'fr', 'es'],
    'es': ['en', 'de', 'fr', 'es'],
}
if SOURCE_LANG_DEFAULT not in SUPPORTED_LANGS.keys():
    raise ValueError(f"default source language \"{SOURCE_LANG_DEFAULT}\" is not supported!")
if TARGET_LANG_DEFAULT not in SUPPORTED_LANGS[SOURCE_LANG_DEFAULT]:
    raise ValueError(f"default target language \"{TARGET_LANG_DEFAULT}\" is not supported for default source language \"{SOURCE_LANG_DEFAULT}\"!")

langid.set_languages(SUPPORTED_LANGS.keys())

models, tokenizers = load_models_and_tokenizers(SUPPORTED_LANGS)

def detect_source_and_target_language(text: str):
    # TODO: this currently uses set source and target languages from configuration
    # this might be extended to use annotations in the triplestore, or other means to pass 
    # source and target language dynamically.

    #if SOURCE_LANG_DEFAULT != None and len(SOURCE_LANG_DEFAULT.strip()) > 0:
    #    logging.info(f"Using SOURCE_LANGUAGE from configuration: {SOURCE_LANG_DEFAULT}")
    #else:
    #    logging.info("No SOURCE_LANGUAGE specified, detecting with langid!")
    #    source_lang, prob = langid.classify(text)
    #    logging.info(f"source language: {source_lang} ({prob} %)")

    return SOURCE_LANG_DEFAULT, TARGET_LANG_DEFAULT


def translate_input(text: str, source_lang: str, target_lang: str) -> str:

    batch = tokenizers[source_lang][target_lang]([text], return_tensors="pt", padding=True)
    # Make sure that the tokenized text does not exceed the maximum
    # allowed size of 512
    batch["input_ids"] = batch["input_ids"][:, :512]
    batch["attention_mask"] = batch["attention_mask"][:, :512]
    # Perform the translation and decode the output
    translation = models[source_lang][target_lang].generate(**batch)
    result = tokenizers[source_lang][target_lang].batch_decode(translation, skip_special_tokens=True)[0]
    return result


@mt_helsinki_nlp_bp.route("/annotatequestion", methods=['POST'])
def qanary_service():
    """the POST endpoint required for a Qanary service"""

    triplestore_endpoint = request.json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request.json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request.json["values"]["urn:qanary#outGraph"]
    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    text = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, graph=triplestore_ingraph)[0]['text']
    question_uri = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, graph=triplestore_ingraph)[0]['uri']
    logging.info(f'Question Text: {text}')

    source_lang, target_lang = detect_source_and_target_language(text)
    result = translate_input(text, source_lang, target_lang)

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
        target_lang=target_lang,
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
        src_lang=source_lang,
        app_name=SERVICE_NAME_COMPONENT
    )

    logging.info(f'SPARQL: {SPARQLqueryAnnotationOfQuestionTranslation}')
    logging.info(f'SPARQL: {SPARQLqueryAnnotationOfQuestionLanguage}')
    # inserting new data to the triplestore
    insert_into_triplestore(triplestore_endpoint, SPARQLqueryAnnotationOfQuestionTranslation)
    insert_into_triplestore(triplestore_endpoint, SPARQLqueryAnnotationOfQuestionLanguage)

    return jsonify(request.get_json())


@mt_helsinki_nlp_bp.route("/", methods=['GET'])
def index():
    """an examplary GET endpoint returning "hello world (String)"""

    logging.info("host_url: %s" % (request.host_url,))
    return "Hi! \n This is Python MT Helsinki NLP component"
