import os
import requests
import json
import logging
from datetime import datetime
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, PlainTextResponse
import uvicorn

from qanary_helpers.registration import Registration
from qanary_helpers.registrator import Registrator
from qanary_helpers.qanary_queries import insert_into_triplestore, get_text_question_in_graph, query_triplestore

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

if not os.getenv("PRODUCTION"):
    from dotenv import load_dotenv
    load_dotenv() # required for debugging outside Docker

SPRING_BOOT_ADMIN_URL = os.environ['SPRING_BOOT_ADMIN_URL']    
SPRING_BOOT_ADMIN_USERNAME = os.environ['SPRING_BOOT_ADMIN_USERNAME']
SPRING_BOOT_ADMIN_PASSWORD = os.environ['SPRING_BOOT_ADMIN_PASSWORD']
SERVER_HOST = os.environ['SERVER_HOST']
SERVER_PORT = os.environ['SERVER_PORT']
KG = os.environ['KG']
SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT'] + "-" + KG
SERVICE_DESCRIPTION_COMPONENT = os.environ['SERVICE_DESCRIPTION_COMPONENT']
FALCON_URL = os.environ['FALCON_URL']

URL_COMPONENT = f"http://{SERVER_HOST}:{SERVER_PORT}"
headers = {'Content-Type': 'application/json'}

app = FastAPI()


@app.post("/annotatequestion")
async def qanary_service(request: Request):
    request_json = await request.json()
    triplestore_endpoint_url = request_json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph_uuid = request_json["values"]["urn:qanary#inGraph"]
    
    # get question text from triplestore
    question_text = get_text_question_in_graph(triplestore_endpoint_url, triplestore_ingraph_uuid)[0]['text']
    question_uri = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint_url, graph=triplestore_ingraph_uuid)[0]['uri']

    logging.info(f"Querying FALCON for question: {question_text}")

    response_json = requests.post(FALCON_URL, headers=headers, data=json.dumps({"text": question_text})).json()

    logging.info(f"FALCON response: {response_json}")

    for relation in response_json[f"relations_{KG}"]:
        # workaround for the falcon response
        if KG == "dbpedia":
            rel = relation[0]
        elif KG == "wikidata":
            rel = relation[1].replace("<", "").replace(">", "")

        SPARQLquery = """
                        PREFIX dbr: <http://dbpedia.org/resource/>
                        PREFIX dbo: <http://dbpedia.org/ontology/>
                        PREFIX qa: <http://www.wdaqua.eu/qa#>
                        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                        INSERT {{
                        GRAPH <{uuid}> {{
                            ?newAnnotation rdf:type qa:AnnotationOfRelation ;
                                oa:hasBody <{relation}> ;
                                qa:score \"1.0\"^^xsd:float ;
                                oa:annotatedAt ?time ;
                                ?newAnnotation oa:annotatedBy <{component}> ;
                                oa:hasTarget <{question_uri}> .
                            }}
                        }}
                        WHERE {{
                            BIND (IRI(str(RAND())) AS ?newAnnotation) .
                            BIND (now() as ?time) 
                        }}
                    """.format(
                        uuid=triplestore_ingraph_uuid,
                        question_uri=question_uri,
                        component=SERVICE_NAME_COMPONENT.replace(" ", "-"),
                        relation=rel)

        insert_into_triplestore(triplestore_endpoint_url,
                                SPARQLquery)  # inserting new data to the triplestore

    return JSONResponse(content=request_json)


@app.get("/health")
def health():
    return PlainTextResponse(content="alive") 


metadata = {
    "start": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "description": SERVICE_DESCRIPTION_COMPONENT,
    "written in": "Python"
}

logging.info(f"component metadata: {str(metadata)}") 

registration = Registration(
    name=SERVICE_NAME_COMPONENT,
    serviceUrl=f"{URL_COMPONENT}",
    healthUrl=f"{URL_COMPONENT}/health",
    metadata=metadata
)

reg_thread = Registrator(SPRING_BOOT_ADMIN_URL, SPRING_BOOT_ADMIN_USERNAME,
                        SPRING_BOOT_ADMIN_PASSWORD, registration)
reg_thread.setDaemon(True)
reg_thread.start()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=int(SERVER_PORT))
