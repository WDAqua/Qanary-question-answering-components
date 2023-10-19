# MT LibreTranslate Component

## Description

MT tool that uses [LibreTranslate](https://libretranslate.com/) to translate questions into English.

Docker Hub image: `qanary/qanary-component-mt-libretranslate`

## Input specification

Not applicable as the textual question is a default parameter.

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:output1> a qa:AnnotationOfQuestionTranslation ;
    oa:hasTarget <urn:myQanaryQuestion>; 
    oa:hasBody "translation_result"@en ;
    oa:annotatedBy <urn:qanary:PythonMTLibreTranslate> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .

<urn:qanary:output2> a qa:AnnotationOfQuestionLanguage .
  oa:hasTarget <urn:myQanaryQuestion> ; 
  oa:hasBody "lang-id"^^xsd:string ;
  oa:annotatedBy <urn:qanary:PythonMTLibreTranslate> ;
  oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Usage

1. Clone the Git repository of the collected Qanary components:

```bash
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```

2. Switch to the component's directory:

```bash
cd Qanary-question-answering-components/qanary-component-Python-MT-LibreTranslate
```

3. Set the environment variables in the `.env` file

```bash
SERVER_PORT=40120
SPRING_BOOT_ADMIN_URL=http://qanary-pipeline-host:40111
SERVER_HOST=http://public-component-host
SPRING_BOOT_ADMIN_USERNAME=admin
SPRING_BOOT_ADMIN_PASSWORD=admin
SERVICE_NAME_COMPONENT=LibreTranslate
TRANSLATE_ENDPOINT=http://localhost:5000/translate
LANGUAGES_ENDPOINT=http://localhost:5000/languages
```

The parameters description:

* `SPRING_BOOT_ADMIN_URL` -- URL of the Qanary pipeline (see Step 1 and Step 2 of the [tutorial](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline))
* `SPRING_BOOT_ADMIN_USERNAME` -- the admin username of the Qanary pipeline
* `SPRING_BOOT_ADMIN_PASSWORD` -- the admin password of the Qanary pipeline
* `SERVER_HOST` -- the host of your Qanary component without protocol prefix (e.g., `http://`). It has to be visible to the Qanary pipeline (i.e., a callback from the Qanary pipeline can be executed).
* `SERVER_PORT` -- the port of your Qanary component (has to be visible to the Qanary pipeline)
* `SERVICE_NAME_COMPONENT` -- the name of your Qanary component (for better identification)
* `TRANSLATE_ENDPOINT` -- the LibreTranslate endpoint to be used for translation
* `LANGUAGES_ENDPOINT` -- the LibreTranslate endpoint returning a list of supported languages

4. pull the LibreTranslate image:

```bash
docker-compose pull libretranslate
```

**Note**: Downloading the required language models might take several minutes during which
the service will be unavailable. Use volumes to cache downloaded models.

**Note**: If you have access to the LibreTranslate API (though an API key), you may 
change the `TRANSLATE_ENDPOINT` to the official URL. In this case, the `libretranslate` service 
from the docker-compose file is not needed.

5. Build the Docker image: 

```bash
docker-compose build 
```

6. Run the latest version with docker-compose:

```bash
docker-compose up latest
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

## Endpoints

* `/about` -- (GET) a short service description
* `/health` -- (GET) the status of the connection to a LibreTranslate service
* `/languages` -- (GET) a list of supported source languages with "en" as target language
* `/annotatequestion` -- (POST) standard endpoint for Qanary components

## How To Test This Component

This component uses the [pytest](https://docs.pytest.org/). 
The necessary environment variables have to be configured in `pytest.ini`.

**Note**: The use of a virtual environment is encouraged for this.

First, install the requirements with `pip install -r requirements.txt`.

Then run the local tests with the command `pytest`.

