#!/bin/bash

# Repository root (this script is invoked from the repo root); used to locate the
# shared coverage config so every component measures coverage the same way.
ROOT="$(pwd)"

declare -A summary
failures=false
# get a list of all Python component directories
# exclude submodules (external component repositories)
components=$(comm -3 <(ls | grep -P "[qQ]anary-component.*Python-[a-zA-Z]+$") <(git config --file .gitmodules --get-regexp path | awk '{ print $2 }'))

# create a super directory to hold virtual environments (for caching)

if [ -d environments ]; then
  echo "External evironment directory exists"
else
  if mkdir environments; then
    echo "External environment directory created"
  else
    echo "External environment directory could not be created"
    exit 4
  fi
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
  # pytest-cov measures code coverage; install it if the component does not pin it
  if ! pip show pytest-cov; then
    echo "Installing pytest-cov manually..."
    pip install pytest-cov
  fi

  # run tests with coverage (per-component coverage.xml + htmlcov; test files are
  # excluded via the shared coveragerc). The coverage flags do not change pytest's
  # exit codes (0 = passed, 5 = no tests collected).
  pytest --cov=. --cov-config="${ROOT}/service_config/coveragerc" \
         --cov-report=xml:coverage.xml --cov-report=html:htmlcov --cov-report=term
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
