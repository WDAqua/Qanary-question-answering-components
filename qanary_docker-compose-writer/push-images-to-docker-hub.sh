#! /bin/bash

# reminder to log into docker hub
FILE="../docker-compose.yml"
USERNAME=heipa
LINE=1

IMAGE_NAMES=()
NEW_IMAGE_NAMES=()

docker images

function listImages() {
  I=0
  for IMAGE_STR in "${IMAGE_NAMES[@]}"
  do
    echo "$I: $IMAGE_STR"
    ((I++))
done
}

while read -r CURRENT_LINE
do
  if [[ $CURRENT_LINE == *"image"* ]];
  then
    IMAGE_STR=${CURRENT_LINE:7}
    IMAGE_NAMES+=("$IMAGE_STR")
  fi
  ((LINE++))
done < $FILE



echo "Images to be re-tagged:"
listImages
echo ""
read -p "Provide space-separated indices of images not to be re-tagged. Skip with empty answer: " ANSWER

for INDEX in $ANSWER
  do
    echo "removing ${IMAGE_NAMES["$INDEX"]}"
    unset IMAGE_NAMES["$INDEX"]
  done

echo "Beginning to re-tag ..."

for IMAGE in "${IMAGE_NAMES[@]}"
do
  NEW_TAG="$USERNAME/$IMAGE"
  docker image tag "$IMAGE" "$NEW_TAG"
  NEW_IMAGE_NAMES+=("$NEW_TAG")
done

echo "${NEW_IMAGE_NAMES[@]}"
