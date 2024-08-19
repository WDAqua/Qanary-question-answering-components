import os
import logging
#from decouple config 


logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

SOURCE_LANG_DEFAULT = os.getenv("SOURCE_LANGUAGE_DEFAULT")
TARGET_LANG_DEFAULT = os.getenv("TARGET_LANGUAGE_DEFAULT")
SUPPORTED_LANGS = {
#   source: targets
    'en': ['de', 'fr', 'ru', 'es'],
    'de': ['en', 'fr', 'es'],
    'fr': ['en', 'de', 'ru', 'es'],
    'ru': ['en', 'fr', 'es'],
    'es': ['en', 'de', 'fr', 'es'],
}


def setup_translation_options() -> dict:

    logging.info("SETTING UP TRANSLATION OPTIONS")
    translation_options = {}

    if SOURCE_LANG_DEFAULT != None and len(SOURCE_LANG_DEFAULT.strip()) > 0:
        if SOURCE_LANG_DEFAULT not in SUPPORTED_LANGS.keys():
            raise ValueError(f"Default source language \"{SOURCE_LANG_DEFAULT}\" is not supported!")
        elif TARGET_LANG_DEFAULT not in SUPPORTED_LANGS[SOURCE_LANG_DEFAULT]:
            raise ValueError(f"Default target language \"{TARGET_LANG_DEFAULT}\" is not supported for default source language \"{SOURCE_LANG_DEFAULT}\"!")
        else:
            logging.info(f"Using specific SOURCE_LANGUAGE from configuration: {SOURCE_LANG_DEFAULT}")
            translation_options[SOURCE_LANG_DEFAULT] = [] # init empty
    else:
        logging.info(f"No SOURCE_LANGUAGE specified. Using all supported source languages: {SUPPORTED_LANGS.keys()}")
        translation_options.update({lang: [] for lang in SUPPORTED_LANGS.keys()}) # init empty 

    if TARGET_LANG_DEFAULT != None and len(TARGET_LANG_DEFAULT.strip()) > 0:
        logging.info(f"Using specific TARGET_LANGUAGE from configuration: {TARGET_LANG_DEFAULT}")
        for lang in translation_options.keys():
            if TARGET_LANG_DEFAULT in SUPPORTED_LANGS[lang]:
                translation_options[lang] = [TARGET_LANG_DEFAULT]
            else:
                logging.warning(f"Specific target language {TARGET_LANG_DEFAULT} is not supported for source language {lang}!. \
                                This language pair will be ignored.")
                translation_options.pop(lang) # remove unsupported language pair
    else:
        print_langs = {s_lang: SUPPORTED_LANGS[s_lang] for s_lang in translation_options.keys()}
        logging.info(f"No TARGET_LANGUAGE specified. Using all supported target languages for determined source languages {print_langs}")
        for lang in translation_options.keys():
            translation_options[lang] = SUPPORTED_LANGS[lang] # use default supported languages

    logging.info(translation_options)
    return translation_options


translation_options = setup_translation_options()
