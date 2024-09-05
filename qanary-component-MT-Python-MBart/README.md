# MT MBart component

## Description

MT tool that uses pre-trained MBart and Mbart-50 models by implemented in transformers library.

Docker Hub image: `qanary/qanary-component-mt-mbart`

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
    oa:annotatedBy <urn:qanary:PythonMTMBart> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Usage

1. Clone the Git repository of the collected Qanary components:

```bash
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```

2. Switch to the component's directory:

```bash
cd Qanary-question-answering-components/qanary-component-Python-MT-MBart
```

3. Set the environment variables in the `.env` file

```bash
SERVER_PORT=40120
SPRING_BOOT_ADMIN_URL=http://qanary-pipeline-host:40111
SERVER_HOST=http://public-component-host
SPRING_BOOT_ADMIN_CLIENT_INSTANCE_SERVICE-BASE-URL=http://public-component-host:40120
SPRING_BOOT_ADMIN_USERNAME=admin
SPRING_BOOT_ADMIN_PASSWORD=admin
SERVICE_NAME_COMPONENT=MT-MBart
SERVICE_DESCRIPTION_COMPONENT=Translates question to English
SOURCE_LANGUAGE=de
TARGET_LANGUAGE=en
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
* `SOURCE_LANGUAGE` -- (optional) the default source language of the translation
* `TARGET_LANGUAGE` -- (optional) the default target language of the translation 

4. Build the Docker image: 

```bash
docker-compose build 
```

5. Run the the component with docker-compose:

```bash
docker-compose up
```

After successful execution, component creates Qanary annotation in the Qanary triplestore:
```
GRAPH <uuid> {
  ?a a qa:AnnotationOfQuestionTranslation .
  ?a  oa:hasTarget <urn:myQanaryQuestion> .
  ?a  oa:hasBody "translation_result"@ISO_639-1 language code
  ?a  oa:annotatedBy <urn:qanary:app_name> .
  ?a  oa:annotatedAt ?time .
}
```

### Support for multiple Source and Target Languages

This component relies on the presence of one of more existing annotations that associate a question text with a language. 
This can be in the form of an `AnnotationOfQuestionLanguage`, as created by LD components, or an `AnnotationOfQuestionTranslation` as created by MT components.

It supports multiple combinations of source and target languages. 
You can specify a desired source and target language independently, or simply use all available language pairings. 

If a `SOURCE_LANGUAGE` is set, then only texts with this specific language are considered for translation. 
If none is set, then all configured source languages will be used to find candidates for translation. 

Similarily, if a `TARGET_LANGUAGE` is set, then texts are only translated into that language. 
If none is set, then the texts are translated into all target languages that are supported for their respective source language. 

Note that while configured source languages naturally determine the possible target languages, 
the configured target languages also determine which source languages can be supported!

### Pre-configured Docker Images

You may use the included file `docker-compose-pairs.yml` to build a list of images that are preconfigured for specific language pairs.
Note that if you intend to use these containers at the same time, you need to assign different `SERVER_PORT` values for each image. 

```bash
docker-compose -f docker-compose-pairs.yml build
```

## How To Test This Component

This component uses the [pytest](https://docs.pytest.org/). 
The necessary environment variables have to be configured in `pytest.ini`.

**Note**: The use of a virtual environment is encouraged for this.

First, install the requirements with `pip install -r requirements.txt`.

Then run the local tests with the command `pytest`.

