package eu.wdaqua.component.opentapioca.ned;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import io.swagger.v3.oas.annotations.Operation;

@RestController
public class OpenTapiocaServiceFetcher {

    private RestTemplate restTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaServiceFetcher.class);

    public OpenTapiocaServiceFetcher(RestTemplateWithCaching restTemplate) {
    	this.restTemplate = restTemplate;
    }
    
    /**
     * internal access to functionality shows a log message even if cached request
     * 
     * @param myQuestion
     * @param endpoint
     * @return
     * @throws IOException
     */
    public JsonArray getJsonFromService(
            String myQuestion, String endpoint) throws IOException {
        logger.info("call OpenTapioca endpoint (wrapper): {} -> question='{}'", endpoint, myQuestion);
    	return this.getJsonFromServiceInternal(myQuestion, endpoint);
    }
    /**
     * Perform a POST request with the provided question to the specified endpoint
     *
     * @param myQuestion the question Text
     * @param endpoint   the endpoint to be used
     * @return resources the query results as JsonArray
     * @throws ClientProtocolException
     * @throws IOException
     */
    @Operation(
            summary = "Query OpenTapioca endpoint", //
            operationId = "getJsonFromService", //
            description = "Perform a POST request with the provided question to the specified endpoint" //
    )
    @PostMapping(value="/fetch", consumes=MediaType.ALL_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    // @Cacheable(value="opentapiocaCache", key = "{#myQuestion, #endpoint}")
    public JsonArray getJsonFromServiceInternal(
            String myQuestion, String endpoint) throws IOException {
    	
        Assert.notNull(myQuestion, "Question must not be null");

    	String uriGetParameter = "query=" + URLEncoder.encode(myQuestion, StandardCharsets.UTF_8.toString());
    	String getUri = endpoint + "?" + uriGetParameter;

        logger.info("call OpenTapioca endpoint (cachable): {}", getUri);
        
        JsonArray resources = new JsonArray();
        
        try {			
	    	ResponseEntity<String> result = restTemplate.getForEntity(getUri, String.class); 
	        
        	if (!(result.getStatusCodeValue() >= 200 && result.getStatusCodeValue() < 400 )) {
        		logger.error("OpenTapioca endpoint responds with HTTP error: {}", result.getStatusCodeValue());
		        logger.error("found {} annotations for \"{}\"", resources.size(), myQuestion);
        		return resources;
        	} else {
		        // parse the response data as JSON to get Wikidata resources
		        JsonElement root = JsonParser.parseString(result.getBody());
		        resources = root.getAsJsonObject().get("annotations").getAsJsonArray();
		        logger.info("found {} annotations for \"{}\"", resources.size(), myQuestion);
        	}
		} catch (Exception e) {
			logger.error("error in getJsonFromService: {}", e.getMessage());
		}

        return resources;
    }

    /**
     * Create FoundWikidataResource objects from the JSON response of OpenTapioca endpoint
     *
     * @param resources JsonArray of resources
     * @return foundWikidataResources list of FoundWikidataResource objects
     * @throws URISyntaxException
     */
    @Operation(
            summary = "Parse the OpenTapioca endpoint reponse", //
            operationId = "parseOpenTapiocaResults", //
            description = "Create FoundWikidataResource objects from the JSON response of OpenTapioca endpoint" //
    )
    public List<FoundWikidataResource> parseOpenTapiocaResults(JsonArray resources) throws URISyntaxException {

        List<FoundWikidataResource> foundWikidataResources = new LinkedList<>();
        logger.info("found {} terms", resources.size());

        for (int i = 0; i < resources.size(); i++) {
            // this layer only contains information about the surface form: the part of the
            // question String for which one or multiple entities were found.

            JsonObject currentTerm = resources.get(i).getAsJsonObject();
            int start = currentTerm.get("start").getAsInt();
            int end = currentTerm.get("end").getAsInt();

            // the second layer contains all entities that were identified for that surface form.
            JsonArray tags = currentTerm.get("tags").getAsJsonArray();
            for (int j = 0; j < tags.size(); j++) {
                JsonObject entity = tags.get(j).getAsJsonObject();
                double score = entity.get("rank").getAsDouble();
                String qid = entity.get("id").getAsString();
                URI resource = new URI("http://www.wikidata.org/entity/" + qid);

                // hold the information for every individual entity
                foundWikidataResources.add(new FoundWikidataResource(start, end, score, resource));
            }
        }
        return foundWikidataResources;
    }
}
