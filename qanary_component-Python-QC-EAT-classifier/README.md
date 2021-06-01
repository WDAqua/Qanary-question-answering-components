# Expected Answer Type (EAT) classification component

The component classifies the answer type of a textual question. 

For example:

* question: "Where was Angela Merkel born?"; 
* answer type: "Place"

The answer type taxonomy is using the top-level classes of the [DBpedia Ontology](http://mappings.dbpedia.org/server/ontology/classes/). Hence, the result is always a DBpedia concept.

The component is integrating a classification model that was pretrained using Tensorflow. The model is served as external component by [Tensorflow Serving](https://www.tensorflow.org/tfx/guide/serving). Hence, this EAT classification is following the structure of a Qanary Wrapper component.

## Usage

1. Clone the Git repository of the collected Qanary components: 

```bash
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```

2. Switch to the component's directory: 

```bash
cd Qanary-question-answering-components/qanary_component-Python-QC-EAT-classifier
```

3. Build the Docker container: 

```bash
docker build -t eat-component:latest .
```

6. Run the Docker container with the following environment variables (here it is assumed that the service uses port 41097, all values can be changed w.r.t. your individual needs):
```bash
docker run -d -p 41097:41097 \
    -e SPRING_BOOT_ADMIN_URL='http://webengineering.ins.hs-anhalt.de:43740' \
    -e SPRING_BOOT_ADMIN_USERNAME='admin' \
    -e SPRING_BOOT_ADMIN_PASSWORD='admin' \
    -e SERVICE_HOST='http://webengineering.ins.hs-anhalt.de' \
    -e SERVICE_PORT=41097 \
    -e SERVICE_NAME_COMPONENT='EAT-Component' \
    -e SERVICE_DESCRIPTION_COMPONENT='EAT-Component-Description' \
    -e CLASSIFICATION_ENDPOINT='http://webengineering.ins.hs-anhalt.de:41066/answer_type_classifier/predict' \
    eat-component:latest
```

The parameters description:

* `SPRING_BOOT_ADMIN_URL` -- URL of the Qanary pipeline (see Step 1 and Step 2 of the [tutorial](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline))
* `SPRING_BOOT_ADMIN_USERNAME` -- the admin username of the Qanary pipeline
* `SPRING_BOOT_ADMIN_PASSWORD` -- the admin password of the Qanary pipeline
* `SERVICE_HOST` -- the host of your Qanary component without protocol prefix (e.g., `http://`). It has to be visible to the Qanary pipeline (i.e., a callback from the Qanary pipeline can be executed).
* `SERVICE_PORT` -- the port of your Qanary component (has to be visible to the Qanary pipeline)
* `SERVICE_NAME_COMPONENT` -- the name of your Qanary component (for better identification)
* `SERVICE_DESCRIPTION_COMPONENT` -- the description of your Qanary component
* `CLASSIFICATION_ENDPOINT` -- the endpoint of the classifier deployed by using [Tensorflow Serving](https://www.tensorflow.org/tfx/guide/serving). This parameter must not be changed unless you have your own endpoint.

After execution component creates Qanary annotations in the Qanary triplestore with the following predicates:

* `qa:hasAnswerType` -- the predicted answer type;
* `oa:annotatedBy` -- the name of the component;
* `oa:annotatedAt` -- the time of the prediction.
