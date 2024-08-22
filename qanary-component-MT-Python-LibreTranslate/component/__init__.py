from fastapi import FastAPI
from fastapi.responses import RedirectResponse, Response, JSONResponse
from component import mt_libretranslate
from component.mt_libretranslate import check_connection
from component.mt_libretranslate import get_languages

version = "0.2.0"

# default config file
configfile = "app.conf"

# service status information
healthendpoint = "/health"
aboutendpoint = "/about"
languagesendpoint = "/languages"
translateendpoint = "/translate"

# init Flask app and add externalized service information
app = FastAPI(docs_url="/swagger-ui.html")
app.include_router(mt_libretranslate.router)


@app.get("/")
async def main():
    return RedirectResponse("/about")


@app.get(healthendpoint)
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    status, message = check_connection()
    return Response(f"LibreTranslate server is {'UP' if status else 'DOWN'} - {message}", media_type="text/plain")

@app.get(aboutendpoint)
def about():
    """required about endpoint for callback of Srping Boot Admin server"""
    return Response("Translates questions into English. \nSee /languages for a list of supported source languages!", media_type="text/plain")

@app.get(languagesendpoint)
def languages():
    return JSONResponse(get_languages())

@app.get(translateendpoint+"_to_one", description="", tags=["Translate"])
def translate_to_one(text: str, source_lang: str, target_lang: str):
    return JSONResponse(translate_to_one(text, source_lang, target_lang))

@app.get(translateendpoint+"_to_all", description="", tags=["Translate"])
def translate_to_all(text: str, source_lang: str):
    return JSONResponse(translate_to_all(text, source_lang))
