from component import mt_helsinki_nlp
from fastapi import FastAPI
from fastapi.responses import RedirectResponse, Response

version = "0.2.0"

# default config file (use -c parameter on command line specify a custom config file)
configfile = "app.conf"

# endpoint for health information of the service required for Spring Boot Admin server callback
HEALTHENDPOINT = "/health"
ABOUTENDPOINT = "/about"
# TODO: add languages endpoint?

# initialize Flask app and add the externalized service information
app = FastAPI(docs_url="/swagger-ui.html")
app.include_router(mt_helsinki_nlp.router)


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
