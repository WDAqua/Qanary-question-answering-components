#!/bin/bash

# TODO: navigate into individual python components, setup venv and run tests, then deactivate

components=$(ls | grep -P "qanary-component.*Python-[a-zA-Z]+$")

for dir in $components
do
  name=$(echo ${dir} | tr "[:upper:]" "[:lower:]")
  echo "Testing ${name} ..."
  cd $dir
  python -m venv env 
  source env/bin/activate
  pip install -r requirements.txt
  if ! pytest;
  then
    echo "Pytest failed"
    deactivate
    exit 4 # stop if test fails
  fi 
  deactivate
  cd ..
done
