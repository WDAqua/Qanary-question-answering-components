import os
import logging
from datetime import datetime
from app import app, configfile, aboutendpoint, healthendpoint
from qanary_helpers.registration import Registration
from qanary_helpers.registrator import Registrator

logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

SPRING_BOOT_ADMIN_URL = os.environ['SPRING_BOOT_ADMIN_URL']
SPRING_BOOT_ADMIN_USERNAME = os.environ['SPRING_BOOT_ADMIN_USERNAME']
SPRING_BOOT_ADMIN_PASSWORD = os.environ['SPRING_BOOT_ADMIN_PASSWORD']
SERVICE_HOST = os.environ['SERVICE_HOST']
SERVICE_PORT = os.environ['SERVICE_PORT']
SERVICE_NAME_COMPONENT = os.environ['SERVICE_NAME_COMPONENT']
SERVICE_DESCRIPTION_COMPONENT = os.environ['SERVICE_DESCRIPTION_COMPONENT']
URL_COMPONENT = f"http://{SERVICE_HOST}:{SERVICE_PORT}"

# define metadata that will be shown in the Spring Boot Admin server UI
metadata = {
    "start": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "description": SERVICE_DESCRIPTION_COMPONENT,
    "about": f"{SERVICE_HOST}:{SERVICE_PORT}{aboutendpoint}",
    "written in": "Python"
}

# initialize the registration object, to be send to the Spring Boot Admin server
registration = Registration(
    name=SERVICE_NAME_COMPONENT,
    serviceUrl=f"{SERVICE_HOST}:{SERVICE_PORT}",
    healthUrl=f"{SERVICE_HOST}:{SERVICE_PORT}{healthendpoint}",
    metadata=metadata
)

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
    app.run(debug=True, port=SERVICE_PORT)
