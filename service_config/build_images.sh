#!/bin/bash
# clone Qanary pipeline
git clone https://github.com/WDAqua/Qanary.git

# subshell building the Qanary pipeline
(
cd Qanary/
mvn --batch-mode clean install -Ddockerfile.skip=true -DskipTests
)

# delete Qanary pipeline repository
rm -rf Qanary/

# replace secrets
if [ -z "$BABELFY_API_KEY" ]
then
  echo "BABELFY_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/API_KEY/$BABELFY_API_KEY/g" ./service_config/files/ned-babelfy
  sed -i "s/API_KEY/$BABELFY_API_KEY/g" ./service_config/files/ner-babelfy
fi

if [ -z "$DANDELION_API_KEY" ]
then
  echo "DANDELION_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/API_KEY/$DANDELION_API_KEY/g" ./service_config/files/ned-dandelion
  sed -i "s/API_KEY/$DANDELION_API_KEY/g" ./service_config/files/ner-dandelion
fi

if [ -z "$MEANINGCLOUD_API_KEY" ]
then
  echo "MEANINGCLOUD_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/API_KEY/$MEANINGCLOUD_API_KEY/g" ./service_config/files/ned-meaningcloud
  sed -i "s/API_KEY/$MEANINGCLOUD_API_KEY/g" ./service_config/files/ner-meaning-cloud
fi

if [ -z "$TAGME_API_KEY" ]
then
  echo "TAGME_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/API_KEY/$TAGME_API_KEY/g" ./service_config/files/ned-tagme
  sed -i "s/API_KEY/$TAGME_API_KEY/g" ./service_config/files/ner-tagme
fi

if [ -z "$TEXTRAZOR_API_KEY" ]
then
  echo "TEXTRAZOR_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/API_KEY/$TEXTRAZOR_API_KEY/g" ./service_config/files/ner-text-razor
fi

if [ -z "$OPENAI_API_KEY" ]
then
  echo "OPENAI_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/OPENAI_API_KEY_PLACEHOLDER/$OPENAI_API_KEY/g" ./service_config/files/tqa-chatgptwrapper
  if [ `grep OPENAI_API_KEY_PLACEHOLDER ./service_config/files/tqa-chatgptwrapper` ]
  then 
    echo "check fails: OPENAI_API_KEY_PLACEHOLDER still in ./service_config/files/tqa-chatgptwrapper"; 
    exit 3 # stop if the API key was not replaced
  else 
    echo "check ok: OPENAI_API_KEY_PLACEHOLDER was replaced in ./service_config/files/tqa-chatgptwrapper"; 
  fi
fi


if [ -z "$OPENAI_API_KEY" ]
then
  echo "OPENAI_API_KEY is not set. Check your secrets."
  exit 2 # stop if no API key is set
else
  sed -i "s/OPENAI_API_KEY_PLACEHOLDER/$OPENAI_API_KEY/g" ./service_config/files/ned-openai-gpt
  # safety check
  if [ `grep OPENAI_API_KEY_PLACEHOLDER ./service_config/files/ned-openai-gpt` ]
  then 
    echo "check fails: OPENAI_API_KEY_PLACEHOLDER still in ned-openai-gpt"; 
    exit 3 # stop if the API key was not replaced
  else 
    echo "check ok: OPENAI_API_KEY_PLACEHOLDER was replaced in ned-openai-gpt"; 
  fi

fi


# build Docker Images and store name and tag
if ! mvn --batch-mode clean package -DskipTests;
then
  echo "Maven build failed"
  exit 4 # stop if maven build fails
fi

docker image ls | grep -oP "^qanary/qanary-component.*\.[0-9]+ " > images.temp

# read image list
images=$(cat images.temp)

i=0

# for each image
for row in $images
do
  # row contains the image name
  if [ $i -eq 0 ]
  then
    # store image name
    file_name=$row
    i=$((i + 1))
  # row contains tag
  else
    # generate version and latest tag
    latest_file_name="${file_name}:latest"
    file_name="${file_name}:${row}"

    i=0

    # tag images and push to Dockerhub
    docker tag "${file_name}" "${latest_file_name}"
    docker push "${file_name}"
    docker push "${latest_file_name}"
  fi
done

# delete temp results
rm images.temp
