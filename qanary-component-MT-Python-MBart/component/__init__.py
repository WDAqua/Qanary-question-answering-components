from component import mt_mbart
from fastapi import FastAPI
from fastapi.responses import RedirectResponse, Response, JSONResponse

version = "0.2.0"

# default config file
configfile = "app.conf"

# service status information
healthendpoint = "/health"
aboutendpoint = "/about"
translateendpoint = "/translate"
# TODO: add languages endpoint?

# init Flask app and add externalized service information
app = FastAPI(docs_url="/swagger-ui.html")
app.include_router(mt_mbart.router)


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

