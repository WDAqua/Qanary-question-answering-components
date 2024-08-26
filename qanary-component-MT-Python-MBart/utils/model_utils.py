from transformers import MBartForConditionalGeneration, MBart50TokenizerFast


def load_models_and_tokenizers():
    """Loads models and tokenizers based on configured translation language pairs.

    Parameters:
    translation_options (dict): Key is the source language, value is a list of configured target languages
    """

    model = MBartForConditionalGeneration.from_pretrained("facebook/mbart-large-50-many-to-many-mmt")
    tokenizer = MBart50TokenizerFast.from_pretrained("facebook/mbart-large-50-many-to-many-mmt")
    return model, tokenizer
