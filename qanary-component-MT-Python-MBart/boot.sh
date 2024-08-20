#!/bin/sh

export $(grep -v '^#' .env | xargs)

echo Downloading the models

python -c "from utils.model_utils import load_models_and_tokenizers; load_models_and_tokenizers(); "

echo Downloading the model finished

echo SERVER_PORT: $SERVER_PORT
echo Qanary pipeline at SPRING_BOOT_ADMIN_URL: $SPRING_BOOT_ADMIN_URL

if [ -n $SERVER_PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app # refer to the gunicorn documentation for more options
fi
