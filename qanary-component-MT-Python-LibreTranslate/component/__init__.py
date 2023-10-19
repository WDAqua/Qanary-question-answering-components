from component.mt_libretranslate import mt_libretranslate_bp
from component.mt_libretranslate import check_connection
from component.mt_libretranslate import get_languages
from flask import Flask

version = "0.1.1"

# default config file
configfile = "app.conf"

# service status information
healthendpoint = "/health"
aboutendpoint = "/about"
languagesendpoint = "/languages"

# init Flask app and add externalized service information
app = Flask(__name__)
app.register_blueprint(mt_libretranslate_bp)

@app.route(healthendpoint, methods=["GET"])
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    status, message = check_connection()
    return f"{'ALIVE' if status else 'DOWN'} - {message}"

@app.route(aboutendpoint, methods=["GET"])
def about():
    """required about endpoint for callback of Srping Boot Admin server"""
    return "Translates questions into English. \nSee /languages for a list of supported source languages!"

@app.route(languagesendpoint, methods=["GET"])
def languages():
    return get_languages()
