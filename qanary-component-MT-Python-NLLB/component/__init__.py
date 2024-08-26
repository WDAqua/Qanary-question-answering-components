from component import mt_nllb
from fastapi import FastAPI
from fastapi.responses import RedirectResponse, Response

version = "0.2.0"

# default config file
configfile = "app.conf"

# service status information
HEALTHENDPOINT = "/health"
ABOUTENDPOINT = "/about"
# TODO: add languages endpoint?

# init Flask app and add externalized service information
app = FastAPI(docs_url="/swagger-ui.html")
app.include_router(mt_nllb.router)

@app.get("/")
async def main():
    return RedirectResponse("/about")

@app.get(HEALTHENDPOINT, description="Shows the status of the component")
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    return Response("alive", media_type="text/plain")

@app.get(ABOUTENDPOINT, description="Shows a description of the component")
def about():
    """required about endpoint for callback of Srping Boot Admin server"""
    return Response("Translates questions into English", media_type="text/plain")
