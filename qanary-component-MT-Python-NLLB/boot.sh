#!/bin/sh
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
# TODO: other?
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

python -c "from utils.model_utils import load_models_and_tokenizers; load_models_and_tokenizers(); "

echo Downloading the model finished

echo The port number is: $SERVER_PORT
echo The host is: $SERVER_HOST
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
exec uvicorn run:app --host 0.0.0.0 --port $SERVER_PORT --log-level warning
