package eu.wdaqua.qanary.component.deeppavlovwrapper.qb.messages;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class DeepPavlovResult {
    private static final Logger logger = LoggerFactory.getLogger(DeepPavlovResult.class);

    private URI endpoint;
    private String language;
    private String question;

    private String sparql;
    private float confidence;

    public DeepPavlovResult(
            JSONArray json, String question, URI endpoint, String language
            ) throws URISyntaxException {

        JsonObject parsedJsonObject = JsonParser.parseString(json.toString()).getAsJsonArray()
            .get(0).getAsJsonObject();

        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        initData(parsedJsonObject);
    }

    public void initData(JsonObject response) {
        logger.debug("responseQuestion: {}", response);

        this.sparql = response.get("sparql_query").getAsString();
        this.confidence = response.get("confidence").getAsFloat();
        logger.debug("sparql: {} (confidence: {})", this.sparql, this.confidence);

        //TODO: in the future this can be extended (or make a separate QBE)
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

    public float getConfidence() {
        return confidence;
    }

}
