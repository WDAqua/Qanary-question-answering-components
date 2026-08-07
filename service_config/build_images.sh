#!/bin/bash

# ensure that submodules are not in maven reactor
submodules=$(git config --file .gitmodules --get-regexp path | awk '{ print $2 }')
for submodule in $submodules; 
do
  if grep -qi $submodule "pom.xml"; then
    echo "Submodules should be tested and built externally. Please remove \"${submodule}\" from the maven reactor list."
    exit 4
  fi
done

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


# build the JARs -- each component's Dockerfile copies target/<finalName>.jar
if ! mvn --batch-mode clean package -DskipTests;
then
  echo "Maven build failed"
  exit 4 # stop if maven build fails
fi

# Maven only produces the JARs; it does not build any Docker image (the components
# carry no Docker plugin). So build every component image here from the component's
# own Dockerfile. list_java_component_images.py derives the image coordinates from
# each pom.xml and fails if no JAR was produced at all, so that a broken build can
# no longer look like a successful run that just happens to push nothing.
if ! python3 service_config/list_java_component_images.py > images.temp;
then
  echo "Could not determine the component images to build"
  rm -f images.temp
  exit 5
fi

built=0
while IFS=$'\t' read -r directory image version jar_file
do
  [ -z "${directory}" ] && continue

  echo "::group::Building ${image}:${version} from ${directory}"
  if ! docker build "${directory}" \
        --file "${directory}/Dockerfile" \
        --build-arg "JAR_FILE=${jar_file}" \
        --tag "${image}:${version}" \
        --tag "${image}:latest";
  then
    echo "::endgroup::"
    echo "Docker build failed for ${directory}"
    rm -f images.temp
    exit 6
  fi
  echo "::endgroup::"

  # push both the versioned and the latest tag to Dockerhub
  if ! docker push "${image}:${version}" || ! docker push "${image}:latest";
  then
    echo "Docker push failed for ${image}"
    rm -f images.temp
    exit 7
  fi

  built=$((built + 1))
done < images.temp

# delete temp results
rm -f images.temp

if [ "${built}" -eq 0 ]
then
  echo "No component image was built - refusing to report success"
  exit 8
fi

echo "Successfully built and pushed ${built} component image(s)"
