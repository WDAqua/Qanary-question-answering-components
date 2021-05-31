# Expected Answer Type (EAT) classification component

The component classifies the answer type of a question. 

For example, question: "Where was Angela Merkel born?"; 
answer type: "Place".

The answer type taxonomy is similar to the top-level classes of the [DBpedia Ontology](http://mappings.dbpedia.org/server/ontology/classes/).

## Usage

1. Clone the repository `git clone https://github.com/WDAqua/Qanary-question-answering-components.git`
2. Change working directory `cd Qanary-question-answering-components/qanary_component-Python-QC-EAT-classifier`
3. Build the Docker container `docker build -t eat-component:latest .`
4. Run the Docker container with the following environment variables (the values can be changed to your individual ones):
```bash
docker run -d -p 41007:8000 \
    -e SPRING_BOOT_ADMIN_URL='http://webengineering.ins.hs-anhalt.de:43740' \
    -e SPRING_BOOT_ADMIN_USERNAME='admin' \
    -e SPRING_BOOT_ADMIN_PASSWORD='admin' \
    -e SERVICE_HOST='127.0.0.1' \
    -e SERVICE_PORT='41098' \
    -e SERVICE_NAME_COMPONENT='EAT-Component' \
    -e SERVICE_DESCRIPTION_COMPONENT='EAT-Component-Description' \
    -e CLASSIFICATION_ENDPOINT='http://webengineering.ins.hs-anhalt.de:41066/answer_type_classifier/predict' \
    eat-component:latest
```

The parameters description:

* `SPRING_BOOT_ADMIN_URL` -- URL of the Qanary pipeline (see Step 1 and Step 2 of the [tutorial](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline))
* `SPRING_BOOT_ADMIN_USERNAME` -- the admin username of the Qanary pipeline
* `SPRING_BOOT_ADMIN_PASSWORD` -- the admin password of the Qanary pipeline
* `SERVICE_HOST` -- the host of your component without protocol prefix (e.g. `http://`). It has to be visible to the Qanary pipeline
* `SERVICE_PORT` -- the port of your component (has to be visible to the Qanary pipeline)
* `SERVICE_NAME_COMPONENT` -- the name of your component
* `SERVICE_DESCRIPTION_COMPONENT` -- the description of your component
* `CLASSIFICATION_ENDPOINT` -- the endpoint of deployed classifier with Tensorflow Serving. This parameter must not be changed unless you have your own endpoint.

After execution component creates the annotations in the graph with the following predicates:

* `qa:hasAnswerType` -- the predicted answer type;
* `oa:annotatedBy` -- the name of the component;
* `oa:annotatedAt` -- the time of the prediction.