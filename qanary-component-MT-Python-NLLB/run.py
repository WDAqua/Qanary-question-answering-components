import logging
import os
from datetime import datetime
from qanary_helpers.registration import Registration
from qanary_helpers.registrator import Registrator

from component import app, HEALTHENDPOINT, ABOUTENDPOINT

logging.basicConfig(level=logging.ERROR)
# TODO: get logger from module
logger = logging.getLogger(__name__)
logger.setLevel(logging.WARNING)

SPRING_BOOT_ADMIN_URL = os.getenv('SPRING_BOOT_ADMIN_URL')
SPRING_BOOT_ADMIN_USERNAME = os.getenv('SPRING_BOOT_ADMIN_USERNAME')
SPRING_BOOT_ADMIN_PASSWORD = os.getenv('SPRING_BOOT_ADMIN_PASSWORD')
SERVER_HOST = os.getenv('SERVER_HOST')
SERVER_PORT = os.getenv('SERVER_PORT')
SERVICE_NAME_COMPONENT = os.getenv('SERVICE_NAME_COMPONENT')
SERVICE_DESCRIPTION_COMPONENT = os.getenv('SERVICE_DESCRIPTION_COMPONENT')
URL_COMPONENT = f"http://{SERVER_HOST}:{SERVER_PORT}"

# define metadata that will be shown in the Spring Boot Admin server UI
metadata = {
    "start": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "description": SERVICE_DESCRIPTION_COMPONENT,
    "about": f"{SERVER_HOST}:{SERVER_PORT}{ABOUTENDPOINT}",
    "written in": "Python"
}

# initialize the registration object, to be send to the Spring Boot Admin server
registration = Registration(
    name=SERVICE_NAME_COMPONENT,
    serviceUrl=f"{SERVER_HOST}:{SERVER_PORT}",
    healthUrl=f"{SERVER_HOST}:{SERVER_PORT}{HEALTHENDPOINT}",
    metadata=metadata
)

logging.info(f"Start registration on: {SPRING_BOOT_ADMIN_URL} with the credentials: {SPRING_BOOT_ADMIN_USERNAME}/{SPRING_BOOT_ADMIN_PASSWORD}")

# start a thread that will contact iteratively the Spring Boot Admin server
registrator_thread = Registrator(
    SPRING_BOOT_ADMIN_URL,
    SPRING_BOOT_ADMIN_USERNAME,
    SPRING_BOOT_ADMIN_PASSWORD,
    registration
)
registrator_thread.setDaemon(True)
registrator_thread.start()

if __name__ == "__main__":
    # start the web service
    if SERVER_PORT == None:
        raise RuntimeError("SERVER_PORT must not be empty!")
