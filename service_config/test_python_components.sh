#!/bin/bash

declare -A summary
failures=false 
# get a list of all Python component directories
# exclude submodules (external component repositories)
components=$(comm -3 <(ls | grep -P "[qQ]anary-component.*Python-[a-zA-Z]+$") <(git config --file .gitmodules --get-regexp path | awk '{ print $2 }'))

# create a super directory to hold virtual environments (for caching)
if mkdir environments; then
  echo "External environment directory created"
else
  echo "External environment directory could not be created"
fi

printf "Found Python components:\n\n${components}\n\n"


# iterate over components 
for dir in $components
do
  # iterate over Python components 
  name=$(echo ${dir} | tr "[:upper:]" "[:lower:]")
  printf "\n\n===== ${name} =====\n\n"

  cd $dir
  # setup virtual environment in external super directory 
  envname=../environments/${name}
  python -m venv ${envname}
  if source ${envname}/bin/activate; then
    pip install -r requirements.txt 
  else
    echo "Something went wrong trying to install requirements! Exiting ..."
    exit 4
  fi

  # install pytest manually if not included in requirements 
  if ! pip show pytest; then
    echo "Installing pytest manually..."
    pip install pytest
  fi 
  if ! pip show pytest-env; then 
    echo "Installing pytest-env manually..."
    pip install pytest-env
  fi

  # run tests 
  pytest
  test_status=$?
  # check exit codes
  if [ $test_status -eq 0 ]; then # all tests successful 
    summary[${name}]="passed"
  elif [ $test_status -eq 5 ]; then # no tests found
    summary[${name}]="no tests"
  else # tests failed or something else went wrong
    summary[${name}]="failed"
    failures=true
  fi 

  #pip freeze | xargs pip uninstall -y TODO: disabled because that would mess with caching
  deactivate
  rm -r ${envname}

  cd ..

done


# print a summary
printf "\n\n===== SUMMARY =====\n\n"
for x in "${!summary[@]}"; do printf "%s\t:\t[%s]\n" "$x" "${summary[$x]}"; done | column -s$'\t' -t
if $failures; then
  printf "\nSome tests failed!\n"
  exit 4
else
  printf "\nTests succeeded\n"
fi 
