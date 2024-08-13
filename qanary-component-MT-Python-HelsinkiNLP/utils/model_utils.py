from transformers.models.marian.modeling_marian import MarianMTModel
from transformers.models.marian.tokenization_marian import MarianTokenizer


def load_models_and_tokenizers(supported_langs: dict):
    models = {}
    tokenizers = {}
    for s_lang in supported_langs.keys():
        lang_models = {t_lang: MarianMTModel.from_pretrained(f'Helsinki-NLP/opus-mt-{s_lang}-{t_lang}') for t_lang in supported_langs[s_lang]}
        lang_tokenizers = {t_lang: MarianTokenizer.from_pretrained(f'Helsinki-NLP/opus-mt-{s_lang}-{t_lang}') for t_lang in supported_langs[s_lang]}
        models[s_lang] = lang_models
        tokenizers[s_lang] = lang_tokenizers
    return models, tokenizers
