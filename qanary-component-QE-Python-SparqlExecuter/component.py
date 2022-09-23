import os
import json
import uvicorn
import logging
from datetime import datetime
from fastapi import FastAPI, Request
from SPARQLWrapper import SPARQLWrapper, JSON
from fastapi.responses import JSONResponse, PlainTextResponse

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
ENDPOINT = os.environ['SPARQL_ENDPOINT']

URL_COMPONENT = f"http://{SERVER_HOST}:{SERVER_PORT}"
headers = {'Content-Type': 'application/json'}

app = FastAPI()

def execute(query: str, endpoint_url: str = ENDPOINT):
    """
    https://dbpedia.org/sparql
    https://query.wikidata.org/bigdata/namespace/wdq/sparql
    """
    agent_header = {'User-Agent': 'wiki_parser_online/0.17.1 (https://deeppavlov.ai;'
                                        ' info@deeppavlov.ai) deeppavlov/0.17.1'}
    try:
        sparql = SPARQLWrapper(endpoint_url)
        sparql.agent = str(agent_header)
        sparql.setQuery(query)
        sparql.setReturnFormat(JSON)
        response = sparql.query().convert()
        return response
    except Exception as e:
        e = str(e)
        logging.error(f"Execute error: {e}")
        if 'MalformedQueryException' in e or 'bad formed' in e:
            logging.error(query + str('\n' + e))
        
        return {'error': e} 

@app.post("/annotatequestion")
async def qanary_service(request: Request):
    request_json = await request.json()
    triplestore_endpoint_url = request_json["values"]["urn:qanary#endpoint"]
    triplestore_ingraph_uuid = request_json["values"]["urn:qanary#inGraph"]
    
    question_uri = get_text_question_in_graph(triplestore_endpoint=triplestore_endpoint_url, graph=triplestore_ingraph_uuid)[0]['uri']

    sparql = """
    PREFIX qa: <http://www.wdaqua.eu/qa#> 
    PREFIX oa: <http://www.w3.org/ns/openannotation/core/> 
    SELECT ?sparql 
    FROM <{uuid}> 
    WHERE {{ 
        ?a a qa:AnnotationOfAnswerSPARQL ;
           qa:hasScore ?score ; 
           oa:annotatedAt ?time .
        OPTIONAL {{ ?a oa:hasBody ?sparql }} 
    }}
    ORDER BY DESC(?score) LIMIT 1
    """.format(uuid=triplestore_ingraph_uuid)

    logging.info(f"Querying for SPARQL queries: {sparql}")

    generated_sparql = query_triplestore(triplestore_endpoint_url, sparql)

    logging.info(f"SPARQL query generated: {generated_sparql}")

    json_string = json.dumps(execute(query=generated_sparql, endpoint_url=ENDPOINT), ensure_ascii=False).replace('\\"',"").replace('"', '\\"')

    SPARQLquery = """
    PREFIX dbr: <http://dbpedia.org/resource/>
    PREFIX dbo: <http://dbpedia.org/ontology/>
    PREFIX qa: <http://www.wdaqua.eu/qa#>
    PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    INSERT {{
    GRAPH <{uuid}> {{
        ?annotationAnswer a qa:AnnotationOfAnswerJson ;
        oa:hasTarget <{question_uri}> ;
        oa:hasBody ?answerJson ;
        oa:annotatedAt ?time ;
        oa:annotatedBy <{component}> .

        ?answer a qa:AnswerJson ;
            rdf:value "{json_string}"^^xsd:string  .
            
        qa:AnswerJson rdfs:subClassOf qa:Answer .
        }}
    }}
    WHERE {{
        BIND (IRI(str(RAND())) AS ?annotationAnswer) .
        BIND (IRI(str(RAND())) AS ?answer) .
        BIND (now() as ?time) 
    }}
    """.format(
        uuid=triplestore_ingraph_uuid,
        question_uri=question_uri,
        component="qanary:" + SERVICE_NAME_COMPONENT.replace(" ", "-"),
        json_string=json_string)

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
