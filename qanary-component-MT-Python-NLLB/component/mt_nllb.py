import logging
import os
from qanary_helpers.qanary_queries import get_text_question_in_graph, insert_into_triplestore
from qanary_helpers.language_queries import get_translated_texts_in_triplestore, get_texts_with_detected_language_in_triplestore, question_text_with_language, create_annotation_of_question_translation
from utils.model_utils import load_models_and_tokenizers
from utils.lang_utils import translation_options, LANG_CODE_MAP
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse


logging.basicConfig(format="%(asctime)s - %(message)s", level=logging.INFO)
router = APIRouter()

SERVICE_NAME_COMPONENT = os.environ["SERVICE_NAME_COMPONENT"]

model, tokenizer = load_models_and_tokenizers()


def translate_input(text: str, source_lang: str, target_lang: str) -> str:
    logging.info(f"translating \"{text}\" from \"{source_lang}\" to \"{target_lang}\"")
    tokenizer.src_lang = LANG_CODE_MAP[source_lang]
    logging.info(f"source language mapped code: {tokenizer.src_lang}")
    batch = tokenizer(text, return_tensors="pt")

    # Make sure that the tokenized text does not exceed the maximum
    # allowed size of 512
    batch["input_ids"] = batch["input_ids"][:, :512]
    batch["attention_mask"] = batch["attention_mask"][:, :512]

    # Perform the translation and decode the output
    generated_tokens = model.generate(
        **batch,
        forced_bos_token_id=tokenizer.convert_tokens_to_ids(LANG_CODE_MAP[target_lang]))
    result = tokenizer.batch_decode(generated_tokens, skip_special_tokens=True)[0]
    translation = result.replace("\"", "\\\"") #keep quotation marks that are part of the translation
    logging.info(f"result: \"{translation}\"")
    return translation


def translate_to_one(text: str, source_lang: str, target_lang: str):
    translation = translate_input(text, source_lang, target_lang)
    return {target_lang: translation}


def translate_to_all(text: str, source_lang: str):
    translations = list()
    for target_lang in translation_options[source_lang]:
        translation = translate_input(text, source_lang, target_lang)
        translations.append({
            target_lang: translation
        })
    return translations


def find_source_texts_in_triplestore(triplestore_endpoint: str, graph_uri: str, lang: str) -> list[question_text_with_language]:
    source_texts = []

    # check if supported languages have been determined already (LD)
    # (use filters)
    # if so, use the target uris to find the question text to translate
    ld_source_texts = get_texts_with_detected_language_in_triplestore(triplestore_endpoint, graph_uri, lang)
    source_texts.extend(ld_source_texts)

    # check if there are translations into the relevant language (MT)
    # (use filters)
    # if so, use the translation texts
    mt_source_texts = get_translated_texts_in_triplestore(triplestore_endpoint, graph_uri, lang)
    source_texts.extend(mt_source_texts)

    # TODO: what if nothing found? 
    if len(source_texts) == 0:
        logging.warning(f"No source texts with language {lang} could be found In the triplestore!")

    return source_texts


@router.post("/annotatequestion", methods=["POST"])
async def qanary_service(request: Request):
    """the POST endpoint required for a Qanary service"""

    request_json = await request.json()

    triplestore_endpoint = request_json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph = request_json["values"]["urn:qanary#inGraph"]
    triplestore_outgraph = request_json["values"]["urn:qanary#outGraph"]
    logging.info("endpoint: %s, inGraph: %s, outGraph: %s" % \
                 (triplestore_endpoint, triplestore_ingraph, triplestore_outgraph))

    text_question_in_graph = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint, graph=triplestore_ingraph)
    question_text = text_question_in_graph[0]['text']
    logging.info(f'Original question text: {question_text}')


    # Collect texts to be translated (group by source language)

    for source_lang in translation_options.keys():
        source_texts = find_source_texts_in_triplestore(
            triplestore_endpoint=triplestore_endpoint,
            graph_uri=triplestore_ingraph,
            lang=source_lang
        )

        # translate source texts into specified target languages
        for target_lang in translation_options[source_lang]:
            for source_text in source_texts:
                translation = translate_input(source_text.get_text(), source_lang, target_lang)
                if len(translation.strip()) > 0:
                    SPARQLqueryAnnotationOfQuestionTranslation = create_annotation_of_question_translation(
                        graph_uri=triplestore_ingraph,
                        question_uri=source_text.get_uri(),
                        translation=translation,
                        translation_language=target_lang,
                        app_name=SERVICE_NAME_COMPONENT
                    )
                    insert_into_triplestore(triplestore_endpoint, SPARQLqueryAnnotationOfQuestionTranslation)
                else:
                    logging.error(f"result is empty string!")

    return JSONResponse(request_json)
