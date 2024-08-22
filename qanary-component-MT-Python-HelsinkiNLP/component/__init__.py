from component import mt_helsinki_nlp
from fastapi import FastAPI
from fastapi.responses import RedirectResponse, Response, JSONResponse

version = "0.2.0"

# default config file (use -c parameter on command line specify a custom config file)
configfile = "app.conf"

# endpoint for health information of the service required for Spring Boot Admin server callback
healthendpoint = "/health"
aboutendpoint = "/about"
translateendpoint = "/translate"
# TODO: add languages endpoint?

# initialize Flask app and add the externalized service information
app = FastAPI(docs_url="/swagger-ui.html")
app.include_router(mt_helsinki_nlp.router)


@app.get("/")
async def main():
    return RedirectResponse("/about")


@app.get(healthendpoint)
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    return Response("alive", media_type="text/plain")

@app.get(aboutendpoint)
def about():
    """required about endpoint for callback of Srping Boot Admin server"""
    return Response("Translates questions into English", media_type="text/plain")

@app.get(translateendpoint+"_to_one", description="", tags=["Translate"])
def translate_to_one(text: str, source_lang: str, target_lang: str):
    return JSONResponse(translate_to_one(text, source_lang, target_lang))

@app.get(translateendpoint+"_to_all", description="", tags=["Translate"])
def translate_to_all(text: str, source_lang: str):
    return JSONResponse(translate_to_all(text, source_lang))

