# REL Python Falcon component

## Description

The component wraps the [Falcon](https://labs.tib.eu/falcon/) relation extraction service.

Docker Hub image: `qanary/qanary-component-rel-python-falcon`

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:output> a qa:AnnotationOfRelation .
<urn:qanary:output> oa:hasTarget [
    a   oa:SpecificResource;
        oa:hasSource    <urn:qanary:myQanaryQuestion> ;
        oa:hasSelector  [
            a oa:TextPositionSelector ;
            oa:start "0"^^xsd:nonNegativeInteger ;
            oa:end  "5"^^xsd:nonNegativeInteger
        ]
    ] .
<urn:qanary:output> oa:hasBody <dbr:Relation> ;
    oa:annotatedBy <urn:qanary:RELPythonFalcon> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Usage

1. Clone the Git repository of the collected Qanary components:

```bash
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```

2. Switch to the component's directory:

```bash
cd Qanary-question-answering-components/qanary-component-REL-Python-Falcon
```

3. Set the environment variables in the `.env` file

```bash
SPRING_BOOT_ADMIN_URL=http://qanary-pipeline-host:40111
SPRING_BOOT_ADMIN_USERNAME=admin
SPRING_BOOT_ADMIN_PASSWORD=admin
SERVER_HOST=public-component-host
SERVER_PORT=40120
SERVICE_NAME_COMPONENT=Falcon REL component
SERVICE_DESCRIPTION_COMPONENT=Identifies and links relations in a given question
KG=dbpedia
FALCON_URL=https://labs.tib.eu/falcon/falcon2/api?mode=long&db=1
PRODUCTION=True
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
* `KG` -- the knowledge graph to be used for relation extraction (only `dbpedia` and `wikidata` are supported)
* `FALCON_URL` -- the URL of the Falcon REST API
* `PRODUCTION` -- the flag that indicates whether the component is running in production mode

4. Build the Docker image: 

```bash
docker-compose build .
```

5. Run the the component with docker-compose:

```bash
docker-compose up
```