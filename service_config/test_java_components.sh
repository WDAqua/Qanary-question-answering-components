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

# Test components with JaCoCo code-coverage instrumentation.
#
# JaCoCo is attached on the command line so no per-component pom needs to change;
# 0.8.15 runs on the Java 25 CI runner while instrumenting the components' Java 21
# bytecode. --fail-at-end keeps testing every module so coverage is still collected
# when a module's tests fail, and the report goal then runs against the collected
# jacoco.exec data (producing target/site/jacoco/jacoco.xml + html per module).
JACOCO="org.jacoco:jacoco-maven-plugin:0.8.15"

mvn --batch-mode --no-transfer-progress --fail-at-end "${JACOCO}:prepare-agent" test
test_status=$?

# Always render the coverage reports from whatever exec data was collected.
# --fail-at-end is essential here: a single module whose report goal errors (e.g.
# QB-Sina ships a shaded jar inside target/classes that JaCoCo can't analyze) must
# not abort the reactor and leave every later module without a report.
mvn --batch-mode --no-transfer-progress --fail-at-end "${JACOCO}:report" || true

if [ $test_status -ne 0 ]; then
  echo "Maven test failed"
  exit 4 # stop if test fails
fi
