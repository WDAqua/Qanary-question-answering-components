import langid
import logging
import os
from flask import Blueprint, jsonify, request
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore
from transformers.models.marian.modeling_marian import MarianMTModel
from transformers.models.marian.tokenization_marian import MarianTokenizer

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)
mt_helsinki_nlp_bp = Blueprint('mt_helsinki_nlp_bp', __name__, template_folder='templates')

SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']
supported_langs = ['ru', 'es', 'de', 'fr']
langid.set_languages(supported_langs)
models = {lang: MarianMTModel.from_pretrained('Helsinki-NLP/opus-mt-{lang}-en'.format(lang=lang)) for lang in supported_langs}
tokenizers = {lang: MarianTokenizer.from_pretrained('Helsinki-NLP/opus-mt-{lang}-en'.format(lang=lang)) for lang in supported_langs}


@mt_helsinki_nlp_bp.route("/annotatequestion", methods=['POST'])
def qanary_service():
    """the POST endpoint required for a Qanary service"""
    
    triplestore_endpoint = request.json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request.json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request.json["values"]["urn:qanary#outGraph"]
    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    text = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, graph=triplestore_ingraph)[0]['text']
    logging.info(f'Question Text: {text}')
    
    lang, prob = langid.classify(text)

    batch = tokenizers[lang]([text], return_tensors="pt", padding=True)
                    
    # Make sure that the tokenized text does not exceed the maximum
    # allowed size of 512
    batch["input_ids"] = batch["input_ids"][:, :512]
    batch["attention_mask"] = batch["attention_mask"][:, :512]
    # Perform the translation and decode the output
    translation = models[lang].generate(**batch)
    result = tokenizers[lang].batch_decode(translation, skip_special_tokens=True)[0]

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
        qanary_question_uri=triplestore_endpoint,
        translation_result=result,
        src_lang=lang,
        app_name="{0}:Python".format(SERVICE_NAME_COMPONENT)
    )
    
    logging.info(f'SPARQL: {SPARQLquery}')
    # inserting new data to the triplestore
    insert_into_triplestore(triplestore_endpoint, SPARQLquery)

    return jsonify(request.get_json())


@mt_helsinki_nlp_bp.route("/", methods=['GET'])
def index():
    """an examplary GET endpoint returning "hello world (String)"""

    logging.info("host_url: %s" % (request.host_url,))
    return "Hi! \n This is Python MT Helsinki NLP component"
