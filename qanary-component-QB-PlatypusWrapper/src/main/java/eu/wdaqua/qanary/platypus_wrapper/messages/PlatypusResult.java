package eu.wdaqua.qanary.platypus_wrapper.messages;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class PlatypusResult {
    private static final Logger logger = LoggerFactory.getLogger(PlatypusResult.class);

    private final com.google.gson.JsonParser jsonParser;

    private final URI endpoint;
    private final String language;
    private final String question;

    private String sparql;
    private List<String> values;
    private String type;
    private URI datatype;
    private double confidence;

    @Hidden
    public final URI RESOURCETYPEURI;
    @Hidden
    public final URI BOOLEANTYPEURI;
    @Hidden
    public final URI STRINGTYPEURI;

    public PlatypusResult(JSONObject json, String question, URI endpoint, String language) throws URISyntaxException {
        jsonParser = new JsonParser();
        JsonObject parsedJsonObject = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonObject("member");

        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");

        initData(parsedJsonObject);
    }

    /**
     * init the fields while parsing the JSON data
     *
     * @param answers
     * @throws URISyntaxException
     */
    private void initData(JsonObject answers) throws URISyntaxException {
        logger.debug("responseQuestion: {}", answers);

        logger.debug("0. sparql: {}", answers.get("platypus:sparql").getAsString());
        logger.debug("0. confidence: {}", answers.get("resultScore").getAsDouble());

        this.confidence = answers.get("resultScore").getAsDouble();
        this.sparql = answers.get("platypus:sparql").getAsString();
    }

    public JsonParser getJsonParser() {
        return jsonParser;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getLanguage() {
        return language;
    }

    public String getQuestion() {
        return question;
    }

    public String getSparql() {
        return sparql;
    }

    public List<String> getValues() {
        return values;
    }

    public String getType() {
        return type;
    }

    public URI getDatatype() {
        return datatype;
    }

    public double getConfidence() {
        return confidence;
    }
}
