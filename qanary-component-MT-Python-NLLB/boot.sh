#!/bin/sh

export $(grep -v '^#' .env | xargs)

echo Downloading the model
python -c 'from transformers import AutoModelForSeq2SeqLM, AutoTokenizer; model = AutoModelForSeq2SeqLM.from_pretrained("facebook/nllb-200-distilled-600M") ; tokenizer = AutoTokenizer.from_pretrained("facebook/nllb-200-distilled-600M")'
echo Downloading the model finished

echo The port number is: $SERVER_PORT
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
if [ -n $SERVER_PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app # refer to the gunicorn documentation for more options
fi
