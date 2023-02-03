#!/bin/sh

export $(grep -v '^#' .env | xargs)

echo Downloading the model
python -c 'from transformers import MBartForConditionalGeneration, MBart50TokenizerFast; model = MBartForConditionalGeneration.from_pretrained("facebook/mbart-large-50-many-to-many-mmt"); tokenizer = MBart50TokenizerFast.from_pretrained("facebook/mbart-large-50-many-to-many-mmt")'
echo Downloading the model finished

echo SERVER_PORT: $SERVER_PORT
echo Qanary pipeline at SPRING_BOOT_ADMIN_URL: $SPRING_BOOT_ADMIN_URL

if [ -n $SERVER_PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app # refer to the gunicorn documentation for more options
fi
