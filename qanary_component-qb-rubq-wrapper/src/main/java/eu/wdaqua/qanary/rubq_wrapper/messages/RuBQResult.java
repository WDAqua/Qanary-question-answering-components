package eu.wdaqua.qanary.rubq_wrapper.messages;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import io.swagger.v3.oas.annotations.Hidden;
import net.minidev.json.JSONObject;

public class RuBQResult {
    private static final Logger logger = LoggerFactory.getLogger(RuBQResult.class);

    private com.google.gson.JsonParser jsonParser;

    private URI endpoint;
    private String language;
    private String question;

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

    public RuBQResult(JSONObject json, String question, URI endpoint, String language) throws URISyntaxException {
        jsonParser = new JsonParser();
        JsonArray parsedJsonArray = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonArray("queries").getAsJsonArray();

        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");

        initData(parsedJsonArray);
    }

    /**
     * init the fields while parsing the JSON data
     *
     * @param answers
     * @throws URISyntaxException
     */
    private void initData(JsonArray answers) throws URISyntaxException {
        logger.debug("responseQuestion: {}", answers);

        logger.debug("0. sparql: {}", answers.get(0).getAsString());
        this.sparql = answers.get(0).getAsString();
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
