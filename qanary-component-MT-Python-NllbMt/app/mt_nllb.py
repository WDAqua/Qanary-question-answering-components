import langid
from langdetect import detect
import logging
import os
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore

from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

logging.basicConfig(format="%(asctime)s - %(message)s", level=logging.INFO)

mt_nllb_bp = Blueprint("mt_nllb_bp", __name__, template_folder="templates")

SERVICE_NAME_COMPONENT = os.environ["SERVICE_NAME_COMPONENT"]

model = AutoModelForSeq2SeqLM.from_pretrained("facebook/nllb-200-distilled-600M")
tokenizer = AutoTokenizer.from_pretrained("facebook/nllb-200-distilled-600M")
lang_code_map = {
    'en': 'eng_Latn',
    'de': 'deu_Latn',
    'ru': 'rus_Cyrl',
    'fr': 'fra_Latn',
    'es': 'spa_Latn',
    'pt': 'por_Latn'
}
target_lang = "en"

supported_langs = lang_code_map.keys() # TODO: check supported languages 
langid.set_languages(supported_langs)


@mt_nllb_bp.route("/annotatequestion", methods=["POST"])
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


    # TODO: maybe check for language annotation
    # if none, use default

    #lang, prob = langid.classify(text)
    lang = detect(text)
    logging.info(f"source language: {lang}")


    ## MAIN FUNCTIONALITY
    tokenizer.src_lang = lang_code_map[lang] 
    logging.info(f"source language mapped code: {tokenizer.src_lang}")
    batch = tokenizer(text, return_tensors="pt")

    # Make sure that the tokenized text does not exceed the maximum
    # allowed size of 512
    batch["input_ids"] = batch["input_ids"][:, :512]
    batch["attention_mask"] = batch["attention_mask"][:, :512]

    # Perform the translation and decode the output
    generated_tokens = model.generate(
        **batch, 
        forced_bos_token_id=tokenizer.lang_code_to_id[lang_code_map[target_lang]]) # TODO: defined target lang
    result = tokenizer.batch_decode(generated_tokens, skip_special_tokens=True)[0] 



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


@mt_nllb_bp.route("/", methods=["GET"])
def index():
    """examplary GET endpoint"""

    logging.info("host_url: %s" % (request.host_url))
    return "Python MT NLLB Qanary component"
