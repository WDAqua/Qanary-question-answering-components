#!/bin/bash
# build Docker Images and store name and tag
mvn clean package -DskipTests
docker image ls | grep -oP "qanary.*\.[0-9] " > images.temp

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
    docker tag "${file_name}" "qanary/${file_name}"
    docker tag "${file_name}" "qanary/${latest_file_name}"
    docker push "qanary/${file_name}"
    docker push "qanary/${latest_file_name}"
  fi
done

# delete temp results
rm images.temp
