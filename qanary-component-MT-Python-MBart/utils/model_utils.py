from transformers import MBartForConditionalGeneration, MBart50TokenizerFast


def load_models_and_tokenizers():
    model = MBartForConditionalGeneration.from_pretrained("facebook/mbart-large-50-many-to-many-mmt")
    tokenizer = MBart50TokenizerFast.from_pretrained("facebook/mbart-large-50-many-to-many-mmt")
    return model, tokenizer
