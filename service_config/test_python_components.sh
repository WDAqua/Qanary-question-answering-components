#!/bin/bash

declare -A summary
failures=false 
components=$(ls | grep -P "qanary-component.*Python-[a-zA-Z]+$")

printf "Found Python components:\n\n${components}\n\n"

for dir in $components
do
  # iterate over Python components 
  name=$(echo ${dir} | tr "[:upper:]" "[:lower:]")
  printf "\n\n===== ${name} =====\n\n"

  # setup virtual environment
  cd $dir
  python -m venv env 
  source env/bin/activate
  pip install -r requirements.txt 

  # run tests 
  pytest
  # check exit codes
  if [ $? -eq 0 ]; then # all tests successful 
    summary[${name}]="passed"
  elif [ $? -eq 5 ]; then # no tests found
    summary[${name}]="no tests"
  else # tests failed or something else went wrong
    summary[${name}]="failed"
    failures=true
  fi 

  # cleanup
  pip install pyclean
  pyclean .
  pip freeze | xargs pip uninstall -y
  deactivate
  rm -r env/
  cd ..

done

# print a summary
printf "\n\n===== SUMMARY =====\n\n"
for x in "${!summary[@]}"; do printf "%s\t:\t[%s]\n" "$x" "${summary[$x]}"; done | column -s$'\t' -t
if [ failures ]; then
  echo "\nSome tests failed!"
  exit 4
else
  echo "\nTests succeeded"
fi 
