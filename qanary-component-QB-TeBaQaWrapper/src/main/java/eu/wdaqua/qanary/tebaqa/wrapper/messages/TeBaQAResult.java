package eu.wdaqua.qanary.tebaqa.wrapper.messages;

import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class TeBaQAResult {
    private static final Logger logger = LoggerFactory.getLogger(TeBaQAResult.class);
    @Hidden
    public final URI RESOURCETYPEURI;
    @Hidden
    public final URI BOOLEANTYPEURI;
    @Hidden
    public final URI STRINGTYPEURI;
    private JsonParser jsonParser;
    private URI endpoint;
    private String language;
    private String question;
    private String sparql;
    private List<String> values;
    private String type;
    private URI datatype;
    private double confidence;

    public TeBaQAResult(JSONObject json, String question, URI endpoint, String language) throws URISyntaxException {
        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");

        initData(json);
    }

    /**
     * init the fields while parsing the JSON data
     *
     * @param answers
     * @throws URISyntaxException
     */
    private void initData(JSONObject answers) throws URISyntaxException {
        logger.debug("responseQuestion: {}", answers);

        this.sparql = answers.get("sparql").toString();
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
