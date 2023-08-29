# MT Helsinki NLP component

## Description

MT tool that uses pre-trained models by Helsinki NLP implemented in transformers library

Docker Hub image: `qanary/qanary-component-mt-python-opusmt`

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
cd Qanary-question-answering-components/qanary-component-Python-MT-Helsinki-NLP
```

3. Set the environment variables in the `.env` file

```bash
SERVER_PORT=40120
SPRING_BOOT_ADMIN_URL=http://qanary-pipeline-host:40111
SERVER_HOST=http://public-component-host
SPRING_BOOT_ADMIN_CLIENT_INSTANCE_SERVICE-BASE-URL=http://public-component-host:40120
SPRING_BOOT_ADMIN_USERNAME=admin
SPRING_BOOT_ADMIN_PASSWORD=admin
SERVICE_NAME_COMPONENT=MT-Helsinki-NLP
SERVICE_DESCRIPTION_COMPONENT=Translates question to English
```

The parameters description:

* `SPRING_BOOT_ADMIN_URL` -- URL of the Qanary pipeline (see Step 1 and Step 2 of the [tutorial](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline))
* `SPRING_BOOT_ADMIN_USERNAME` -- the admin username of the Qanary pipeline
* `SPRING_BOOT_ADMIN_PASSWORD` -- the admin password of the Qanary pipeline
* `SERVER_HOST` -- the host of your Qanary component without protocol prefix (e.g., `http://`). It has to be visible to the Qanary pipeline (i.e., a callback from the Qanary pipeline can be executed).
* `SERVER_PORT` -- the port of your Qanary component (has to be visible to the Qanary pipeline)
* `SPRING_BOOT_ADMIN_CLIENT_INSTANCE_SERVICE-BASE-URL` -- the URL of your Qanary component (has to be visible to the Qanary pipeline)
* `SERVICE_NAME_COMPONENT` -- the name of your Qanary component (for better identification)
* `SERVICE_DESCRIPTION_COMPONENT` -- the description of your Qanary component

4. Build the Docker image: 

```bash
docker-compose build .
```

5. Run the the component with docker-compose:

```bash
docker-compose up
```

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

## How To Test This Component

This component uses the [pytest](https://docs.pytest.org/). 
The necessary environment variables have to be configured in `pytest.ini`.

**Note**: The use of a virtual environment is encouraged for this.

First, install the requirements with `pip install -r requirements.txt`.

Then run the local tests with the command `pytest`.

