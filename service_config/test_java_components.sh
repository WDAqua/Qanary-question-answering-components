#!/bin/bash
# clone Qanary pipeline
git clone https://github.com/WDAqua/Qanary.git

# subshell building the Qanary pipeline
(
cd Qanary/
mvn --batch-mode clean install -Ddockerfile.skip=true -DskipTests -Dgpg.skip=true
)

# delete Qanary pipeline repository
rm -rf Qanary/

# test components
if ! mvn --batch-mode --no-transfer-progress test;
then
  echo "Maven test failed"
  exit 4 # stop if test fails
fi 
