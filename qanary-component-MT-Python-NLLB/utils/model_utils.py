from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

def load_models_and_tokenizers():
    """Loads models and tokenizers based on configured translation language pairs.

    Parameters:
    translation_options (dict): Key is the source language, value is a list of configured target languages
    """

    model = AutoModelForSeq2SeqLM.from_pretrained("facebook/nllb-200-distilled-600M")
    tokenizer = AutoTokenizer.from_pretrained("facebook/nllb-200-distilled-600M")
    return model, tokenizer
