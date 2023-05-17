package eu.wdaqua.qanary.component.platypuswrapper.qb.messages;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.swagger.v3.oas.annotations.Hidden;
import net.minidev.json.JSONObject;

public class PlatypusResult {
    private static final Logger logger = LoggerFactory.getLogger(PlatypusResult.class);

    private final com.google.gson.JsonParser jsonParser;

    private final URI endpoint;
    private final String language;
    private final String question;

    private String sparql;
    private List<String> values;
//    private String type;
    private URI datatype;
    private double confidence;

    @Hidden
    public final URI RESOURCETYPEURI;
    @Hidden
    public final URI BOOLEANTYPEURI;
    @Hidden
    public final URI STRINGTYPEURI;
    @Hidden
    public final URI DATETYPEURI;
    @Hidden
    public final URI FLOATTYPEURI;
    @Hidden
    public final URI INTEGERTYPEURI;
    @Hidden
    public final String wikidataResourceUrl;

    public PlatypusResult(JSONObject json, String question, URI endpoint, String language) throws URISyntaxException, DataNotProcessableException {
        jsonParser = new JsonParser();

        logger.info("parsing platypus result...");
        JsonElement parsedJsonElement = jsonParser.parse(json.toJSONString());
        JsonObject jsonObject1 = parsedJsonElement.getAsJsonObject();

        //JsonObject parsedJsonObject = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonObject("member");


        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        this.wikidataResourceUrl = "http://www.wikidata.org/entity/";

        this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        // literal type URIs
        this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");
        this.DATETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#date");
        this.FLOATTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#float");
        this.INTEGERTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#integer");

        try {
            JsonArray parsedJsonArray = jsonObject1.getAsJsonArray("member");
            initData(parsedJsonArray);
		} catch (Exception e) {
            JsonObject parsedJsonObject = jsonObject1.getAsJsonObject("member");
            initData(parsedJsonObject);
		}
    }


    // process data and assign datatype
    private ProcessedResult getData(JsonObject answer) throws DataNotProcessableException {
        // try data literal
        try {
            logger.debug("trying to process data as type 'literal'");
            // get data literal 
            ProcessedResult result = getDataLiteral(answer);
            logger.debug("processed data as type 'literal'");
            return result;
        } catch (Exception e) {
            logger.debug("failed to process data ast type 'literal': {}", e.getMessage());
        }
        //try data resource
        try {
            logger.debug("trying to process data as type 'resource'");
            // get data literal 
            ProcessedResult result = getDataResource(answer);
            logger.debug("processed data as type 'resource'");
            return result;
        } catch (Exception e) {
            logger.debug("failed to process data ast type 'resource': {}", e.getMessage());
        }
        // try data boolean
        try {
            logger.debug("trying to process data as type 'boolean'");
            // get data literal 
            ProcessedResult result = getDataBoolean(answer);
            logger.debug("processed data as type 'boolean'");
            return result;
        } catch (Exception e) {
            logger.debug("failed to process data ast type 'boolean': {}", e.getMessage());
        }
        throw new DataNotProcessableException();
    }


    private ProcessedResult getDataLiteral(JsonObject answer) throws NoLiteralTypeResourceFoundException {
        JsonObject result = answer.getAsJsonObject("result");
        JsonObject valueObject = result.get("rdf:value").getAsJsonObject();
        
        String value = valueObject.get("@value").getAsString();
        String type = valueObject.get("@type").getAsString();
        URI typeUri = getLiteralTypeResource(type);

        logger.debug("found result value: {} ({})", value, typeUri);

        return new ProcessedResult(value, typeUri);
    }

    private ProcessedResult getDataResource(JsonObject answer) throws URISyntaxException {
        JsonObject result = answer.getAsJsonObject("result");
        String resourceString = result.get("@id").getAsString();
        String resourceUrl = resourceString.replace("wd:", wikidataResourceUrl);

        logger.debug("found result value: {} ({})", resourceUrl, RESOURCETYPEURI);

        return new ProcessedResult(resourceString, RESOURCETYPEURI);
    }

    private ProcessedResult getDataBoolean(JsonObject answer) {
        // TODO: find working example and implement
        logger.debug("NOT IMPLEMENTED");
        throw new RuntimeException("method 'getDataBoolean' is not implemented.");
    }

    private URI getLiteralTypeResource(String type) throws NoLiteralTypeResourceFoundException {
        if (type.equals("xsd:string")) {
            return this.STRINGTYPEURI;
        } else if (type.equals("xsd:date")) {
            return this.DATETYPEURI;
        } else if (type.equals("xsd:float")) {
            return this.FLOATTYPEURI;
        } else if (type.equals("xsd:integer")) {
            return this.INTEGERTYPEURI;
        } else {
            throw new NoLiteralTypeResourceFoundException(type);
        }
    } 


    /**
     * init the fields while parsing the JSON data if the value was an array
     *
     * @param answers
     * @throws URISyntaxException
     * @throws DataNotProcessableException
     */
    private void initData(JsonArray answersArray) throws URISyntaxException, DataNotProcessableException {
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
     * @throws DataNotProcessableException
     */
    private void initData(JsonObject answer) throws URISyntaxException, DataNotProcessableException {
        logger.debug("responseQuestion: {}", answer);

        logger.debug("sparql: {}", answer.get("platypus:sparql").getAsString());
        logger.debug("confidence: {}", answer.get("resultScore").getAsDouble());

        this.confidence = answer.get("resultScore").getAsDouble();
        this.sparql = answer.get("platypus:sparql").getAsString();    	
        
        ProcessedResult result = getData(answer);

        // because of method signature it is assumed that no other values are added
        this.values = Arrays.asList(result.getValue());
        this.datatype = result.getDataType();
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

//    public String getType() {
//        return type;
//    }

    public URI getDatatype() {
        return datatype;
    }

    public double getConfidence() {
        return confidence;
    }
}
