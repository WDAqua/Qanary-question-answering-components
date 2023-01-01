#!/bin/bash

cd ..

images=$(ls | grep -P "qanary-component.*Python-[a-zA-Z]+$")

for dir in $images
do
    image=$(echo ${dir} | tr "[:upper:]" "[:lower:]")

    echo "Building ${image}"
    cd $dir
    version=$(grep -oP '(?<=version = ")[^"]*' component/__init__.py)
    latest_image_name="qanary/${image}:latest"
    versioned_image_name="qanary/${image}:${version}"
    docker build -t "${versioned_image_name}" .
    docker tag "${versioned_image_name}" "${latest_image_name}"
    echo "Pushing ${versioned_image_name} and ${latest_image_name}"
    docker push "${versioned_image_name}"
    docker push "${latest_image_name}"
    cd ..
done
