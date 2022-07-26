# MT Helsinki NLP component

## Description

MT tool that uses pre-trained models by Helsinki NLP implemented in transformers library

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:output1> a qa:AnnotationOfQuestionTranslation ;
    oa:hasTarget <urn:myQanaryQuestion>; 
    oa:hasBody "translation_result"@en ;
    oa:annotatedBy <urn:qanary:PythonMTHelsinkiNLP> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .

<urn:qanary:output2> a qa:AnnotationOfQuestionLanguage .
  oa:hasTarget <urn:myQanaryQuestion> ; 
  oa:hasBody "lang-id"^^xsd:string ;
  oa:annotatedBy <urn:qanary:PythonMTHelsinkiNLP> ;
  oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Usage

1. Clone the Git repository of the collected Qanary components:

```bash
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```

2. Switch to the component's directory:

```bash
cd Qanary-question-answering-components/qanary_component-Python-MT-Helsinki-NLP
```

3. Build the Docker container: 

```bash
docker build -t mt-helsinki-nlp-component:latest .
```

6. Run the Docker container with the following environment variables (here it is assumed that the service uses port 41062, all values can be changed w.r.t. your individual needs):
```bash
docker run -d -p 41062:41062 \
    -e SPRING_BOOT_ADMIN_URL='https://webengineering.ins.hs-anhalt.de:43740' \
    -e SPRING_BOOT_ADMIN_USERNAME='admin' \
    -e SPRING_BOOT_ADMIN_PASSWORD='admin' \
    -e SERVICE_HOST='http://webengineering.ins.hs-anhalt.de' \
    -e SERVICE_PORT=41062 \
    -e SERVICE_NAME_COMPONENT='MT-Helsinki-NLP-Component' \
    -e SERVICE_DESCRIPTION_COMPONENT='MT tool that uses pre-trained models by Helsinki NLP implemented in transformers library' \
    -v /data/huggingface-docker-cache:/root/.cache/huggingface/transformers \
    mt-helsinki-nlp-component:latest
```

The parameters description:

* `SPRING_BOOT_ADMIN_URL` -- URL of the Qanary pipeline (see Step 1 and Step 2 of the [tutorial](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline))
* `SPRING_BOOT_ADMIN_USERNAME` -- the admin username of the Qanary pipeline
* `SPRING_BOOT_ADMIN_PASSWORD` -- the admin password of the Qanary pipeline
* `SERVICE_HOST` -- the host of your Qanary component without protocol prefix (e.g., `http://`). It has to be visible to the Qanary pipeline (i.e., a callback from the Qanary pipeline can be executed).
* `SERVICE_PORT` -- the port of your Qanary component (has to be visible to the Qanary pipeline)
* `SERVICE_NAME_COMPONENT` -- the name of your Qanary component (for better identification)
* `SERVICE_DESCRIPTION_COMPONENT` -- the description of your Qanary component

After execution, component creates Qanary annotation in the Qanary triplestore:
```
GRAPH <uuid> {
        ?a a qa:AnnotationOfQuestionLanguage .
        ?a qa:translationResult "translation result" .
        ?a qa:sourceLanguage "ISO_639-1 language code" .
        ?a oa:annotatedBy <urn:qanary:app_name> .
        ?a oa:annotatedAt ?time .
    }
}
```