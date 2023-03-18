package eu.wdaqua.qanary.component.platypuswrapper.qb.messages;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
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

        logger.info("parsing platypus result...");
        JsonElement parsedJsonElement = jsonParser.parse(json.toJSONString());
        JsonObject jsonObject1 = parsedJsonElement.getAsJsonObject();

        //JsonObject parsedJsonObject = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonObject("member");


        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");

        try {
            JsonArray parsedJsonArray = jsonObject1.getAsJsonArray("member");
            initData(parsedJsonArray);
		} catch (Exception e) {
            JsonObject parsedJsonObject = jsonObject1.getAsJsonObject("member");
            initData(parsedJsonObject);
		}
    }

    /**
     * init the fields while parsing the JSON data if the value was an array
     *
     * @param answers
     * @throws URISyntaxException
     */
    private void initData(JsonArray answersArray) throws URISyntaxException {
        JsonObject answer = answersArray.get(0).getAsJsonObject();
        logger.debug("found an answer array, processing just the first result");
        this.initData(answer);
    }
    
    /**
     * 
     * init the fields while parsing the JSON data if the value was an object
     * 
     * @param answer
     * @throws URISyntaxException
     */
    private void initData(JsonObject answer) throws URISyntaxException {
        logger.debug("responseQuestion: {}", answer);

        logger.debug("sparql: {}", answer.get("platypus:sparql").getAsString());
        logger.debug("confidence: {}", answer.get("resultScore").getAsDouble());

        this.confidence = answer.get("resultScore").getAsDouble();
        this.sparql = answer.get("platypus:sparql").getAsString();    	
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
