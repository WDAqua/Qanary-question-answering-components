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

# test components
if ! mvn --batch-mode --no-transfer-progress test;
then
  echo "Maven test failed"
  exit 4 # stop if test fails
fi 
