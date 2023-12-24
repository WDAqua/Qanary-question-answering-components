#!/bin/sh


export $(grep -v '^#' .env | xargs)

echo Downloading the model
python -c "from transformers.models.marian.modeling_marian import MarianMTModel; from transformers.models.marian.tokenization_marian import MarianTokenizer; supported_langs = ['ru', 'es', 'de', 'fr']; models = {lang: MarianMTModel.from_pretrained('Helsinki-NLP/opus-mt-{lang}-en'.format(lang=lang)) for lang in supported_langs}; tokenizers = {lang: MarianTokenizer.from_pretrained('Helsinki-NLP/opus-mt-{lang}-en'.format(lang=lang)) for lang in supported_langs}"
echo Downloading the model finished


echo The port number is: $SERVER_PORT
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
if [ -n $SERVER_PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app # refer to the gunicorn documentation for more options
fi
