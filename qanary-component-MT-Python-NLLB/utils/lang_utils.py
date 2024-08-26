import os
import logging


logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

SOURCE_LANGUAGE = os.getenv("SOURCE_LANGUAGE")
TARGET_LANGUAGE = os.getenv("TARGET_LANGUAGE")
SUPPORTED_LANGS = {
#   source: targets
    'en': ['de', 'ru', 'fr', 'es', 'pt'],
    'de': ['en', 'ru', 'fr', 'es', 'pt'],
    'ru': ['en', 'de', 'fr', 'es', 'pt'],
    'fr': ['en', 'de', 'ru', 'es', 'pt'],
    'es': ['en', 'de', 'ru', 'fr', 'pt'],
    'pt': ['en', 'de', 'ru', 'fr', 'es']
}

LANG_CODE_MAP = {
    'en': 'eng_Latn',
    'de': 'deu_Latn',
    'ru': 'rus_Cyrl',
    'fr': 'fra_Latn',
    'es': 'spa_Latn',
    'pt': 'por_Latn'
}


def setup_translation_options() -> dict:
    """Creates a dictionary of possible source and target languages, based on SUPPORTED_LANGS and configured languages."""

    logging.info("SETTING UP TRANSLATION OPTIONS")
    translation_options = dict() # init emtpy

    # check if a source language is specified
    if SOURCE_LANGUAGE != None and len(SOURCE_LANGUAGE.strip()) > 0:
        # pre-select appropriate translation options from the list of supported source languages
        try:
            translation_options[SOURCE_LANGUAGE] = SUPPORTED_LANGS[SOURCE_LANGUAGE]
        # this will fail for invalid keys!
        except KeyError:
            raise ValueError(f"The source language \"{SOURCE_LANGUAGE}\" is not supported!")
    # if no source language is specified, use all source languages that are supported by the models
    else:
        translation_options = SUPPORTED_LANGS

    # check if a target language is specified
    if TARGET_LANGUAGE != None and len(TARGET_LANGUAGE.strip()) > 0:
        discard_keys = list()
        # remove instances where source == target
        translation_options.pop(TARGET_LANGUAGE, None)
        for source_lang in translation_options.keys():
            if TARGET_LANGUAGE in translation_options[source_lang]:
                translation_options[source_lang] = [TARGET_LANGUAGE]
            else:
                discard_keys.append(source_lang)
        # cleanup keys
        translation_options = {sl:tl for sl,tl in translation_options.items() if sl not in discard_keys}
        # check for empty translation options, if all keys dropped
        if len(translation_options.keys()) == 0:
            raise ValueError("The target language \"{tl}\" is not supported for any configured source languages! \nValid language pairs (source: [targets]) are: \n{slk}!"
                             .format(tl=TARGET_LANGUAGE, slk=SUPPORTED_LANGS))
        # check if only some keys dropped
        elif len(discard_keys) > 0:
            logging.warning("Specific target language \"{tl}\" is not supported for these source languages: {dk}!. \nThese language pairs will be ignored."
                            .format(tl=TARGET_LANGUAGE, dk=discard_keys))
    # else do nothing, the lists are already complete

    logging.info(translation_options)
    return translation_options


translation_options = setup_translation_options()
