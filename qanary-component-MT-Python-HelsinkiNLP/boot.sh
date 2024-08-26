#!/bin/bash
export $(grep -v "^#" < .env)

# check required parameters
declare -a required_vars=(
"SPRING_BOOT_ADMIN_URL"
"SERVER_HOST"
"SERVER_PORT"
"SPRING_BOOT_ADMIN_USERNAME"
"SPRING_BOOT_ADMIN_PASSWORD"
"SERVICE_NAME_COMPONENT"
"SERVICE_DESCRIPTION_COMPONENT"
)

for param in ${required_vars[@]};
do 
    if [[ -z ${!param} ]]; then
        echo "Required variable \"$param\" is not set!"
        echo "The required variables are: ${required_vars[@]}"
        exit 4
    fi
done

echo Downloading the models

python -c "from utils.model_utils import load_models_and_tokenizers; SUPPORTED_LANGS = { 'en': ['de', 'fr', 'ru', 'es'], 'de': ['en', 'fr', 'es'], 'fr': ['en', 'de', 'ru', 'es'], 'ru': ['en', 'fr', 'es'], 'es': ['en', 'de', 'fr', 'es'], }; load_models_and_tokenizers(SUPPORTED_LANGS); "

echo Downloading the model finished

echo The port number is: $SERVER_PORT
echo The host is: $SERVER_HOST
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
exec uvicorn run:app --host 0.0.0.0 --port $SERVER_PORT --log-level warning
