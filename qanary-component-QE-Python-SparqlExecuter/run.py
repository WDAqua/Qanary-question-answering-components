import os 
import logging
from datetime import datetime

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from qanary_helpers.registrator import Registrator
from qanary_helpers.registration import Registration

from component import qe_sparqlexecuter, version


SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']
SERVICE_DESCRIPTION_COMPONENT = os.environ['SERVICE_DESCRIPTION_COMPONENT']
SERVER_HOST = os.environ['SERVER_HOST']
SERVER_PORT = os.environ['SERVER_PORT']
SPRING_BOOT_ADMIN_URL = os.environ['SPRING_BOOT_ADMIN_URL']
SPRING_BOOT_ADMIN_USERNAME = os.environ['SPRING_BOOT_ADMIN_USERNAME']
SPRING_BOOT_ADMIN_PASSWORD = os.environ['SPRING_BOOT_ADMIN_PASSWORD']
URL_COMPONENT = f"http://{SERVER_HOST}:{SERVER_PORT}"

metadata = {
    "start": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "description": SERVICE_DESCRIPTION_COMPONENT,
    "written in": "Python"
}

logging.info(f"component metadata: {str(metadata)}") 

app = FastAPI(
    title=SERVICE_NAME_COMPONENT,
    version=version,
    description=SERVICE_DESCRIPTION_COMPONENT
)

app.include_router(qe_sparqlexecuter.router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
