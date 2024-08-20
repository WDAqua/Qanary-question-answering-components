from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

def load_models_and_tokenizers():
    model = AutoModelForSeq2SeqLM.from_pretrained("facebook/nllb-200-distilled-600M")
    tokenizer = AutoTokenizer.from_pretrained("facebook/nllb-200-distilled-600M")
    return model, tokenizer
