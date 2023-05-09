from component.mt_nllb import mt_nllb_bp
from flask import Flask

version = "0.1.1"

# default config file
configfile = "app.conf"

# service status information
healthendpoint = "/health"

aboutendpoint = "/about"

# init Flask app and add externalized service information
app = Flask(__name__)
app.register_blueprint(mt_nllb_bp)

@app.route(healthendpoint, methods=["GET"])
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    return "alive"

@app.route(aboutendpoint, methods=["GET"])
def about():
    """required about endpoint for callback of Srping Boot Admin server"""
    return "about" # TODO: replace this with a service description from configuration
