from flask import Flask, render_template
from app.answer_type_classifier import answer_type_classifier

# default config file (use -c parameter on command line specify a custom config file)
configfile = "app.conf"

# endpoint for Web page containing information about the service
aboutendpoint = "/about"

# endpoint for health information of the service required for Spring Boot Admin server callback
healthendpoint = "/health"

# initialize Flask app and add the externalized service information
app = Flask(__name__)
app.register_blueprint(answer_type_classifier)


@app.route(healthendpoint, methods=['GET'])
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    return "alive"


@app.route(aboutendpoint)
def about():
    """optional endpoint for serving a web page with information about the web service"""
    return render_template("about.html")
