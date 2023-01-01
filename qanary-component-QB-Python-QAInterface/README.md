# QA Interface Component

## Description

QA Interface component is a customizable wrapper for black-box KGQA systems

## Input specification

Comment: **optional**

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:input> a qa:AnnotationOfQuestionTranslation ;
    oa:hasTarget <urn:myQanaryQuestion>;
    oa:hasBody "translation_result"@en .
```

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:output> a qa:AnnotationOfAnswerSPARQL ;
    oa:hasTarget <urn:qanary:myQanaryQuestion> ;
    oa:hasBody "sparql query"^^xsd:string ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    oa:annotatedBy <urn:qanary:QBPythonQAInterface > .
```

## Usage

1. Clone the Git repository of the collected Qanary components:

```bash
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```

2. Switch to the component's directory:

```bash
cd Qanary-question-answering-components/qanary_component-Python-QA-Interface
```

3. Build the Docker container: 

```bash
docker build -t qa-interface-component:latest .
```

6. Run the Docker container with the following environment variables (here it is assumed that the service uses port 41097, all values can be changed w.r.t. your individual needs):
```bash
docker run -d -p 41060:41060 \
    -e SPRING_BOOT_ADMIN_URL='https://webengineering.ins.hs-anhalt.de:43740' \
    -e SPRING_BOOT_ADMIN_USERNAME='admin' \
    -e SPRING_BOOT_ADMIN_PASSWORD='admin' \
    -e SERVICE_HOST='http://webengineering.ins.hs-anhalt.de' \
    -e SERVICE_PORT=41060 \
    -e SERVICE_NAME_COMPONENT='QA-Interface-Component' \
    -e SERVICE_DESCRIPTION_COMPONENT='QA Interface component is a customizable wrapper for black-box KGQA systems' \
    -e QA_SYSTEM_CONFIG='app/config.json' \
    -e QA_SYSTEM_NAME='QAnswer' \
    qa-interface-component:latest
```

The parameters description:

* `SPRING_BOOT_ADMIN_URL` -- URL of the Qanary pipeline (see Step 1 and Step 2 of the [tutorial](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline))
* `SPRING_BOOT_ADMIN_USERNAME` -- the admin username of the Qanary pipeline
* `SPRING_BOOT_ADMIN_PASSWORD` -- the admin password of the Qanary pipeline
* `SERVICE_HOST` -- the host of your Qanary component without protocol prefix (e.g., `http://`). It has to be visible to the Qanary pipeline (i.e., a callback from the Qanary pipeline can be executed).
* `SERVICE_PORT` -- the port of your Qanary component (has to be visible to the Qanary pipeline)
* `SERVICE_NAME_COMPONENT` -- the name of your Qanary component (for better identification)
* `SERVICE_DESCRIPTION_COMPONENT` -- the description of your Qanary component
* `QA_SYSTEM_CONFIG` -- a configuration for the component (see example below).

After execution component creates Qanary annotation in the Qanary triplestore:
```
GRAPH <uuid> {
    ?a a qa:AnnotationOfQaInterface .
    ?a qa:QaSystemName qa_system_name .
    ?a qa:QaSystemURL qa_system_url .
    ?a qa:answerResults results .
    ?a oa:annotatedBy <urn:qanary:app_name> .
    ?a oa:annotatedAt ?time .
    }
}
```


### Configuration example

The `config.json` file is a default configuration file for the component. The example of its content is as follows:

```json
{
    "QAnswer": {
        "QA_SYSTEM_URL": "https://webengineering.ins.hs-anhalt.de:41021/qanswer/answer?question={question}&lang={lang}&kb={kb}",
        "QA_SYSTEM_PARAMS": {
            "question": "to be replaced",
            "lang": "en",
            "kb": "dbpedia"
        }
    }
}
```

The URL should accept GET requests. Other available systems can be found here: https://webengineering.ins.hs-anhalt.de:41021/docs