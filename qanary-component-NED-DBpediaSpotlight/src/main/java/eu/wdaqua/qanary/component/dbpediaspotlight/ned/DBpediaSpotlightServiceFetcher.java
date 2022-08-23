package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import net.minidev.json.JSONObject;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DBpediaSpotlightServiceFetcher {
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcher.class);

    /**
     * fetch data from the configured DBpedia Spotlight endpoint
     *
     * @param myQanaryQuestion
     * @param myQanaryUtils
     * @param myQuestion
     * @param endpoint
     * @param minimumConfidence
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public JsonArray getJsonFromService(String myQuestion, String endpoint, float minimumConfidence, RestTemplate restTemplate, CacheOfRestTemplateResponse myCacheOfResponses) throws UnsupportedEncodingException, URISyntaxException {

        String uriGetParameter = "?text=" + URLEncoder.encode(myQuestion, StandardCharsets.UTF_8.toString()) + "&confidence=" + minimumConfidence;

        URI uri = new URI(endpoint + uriGetParameter);

        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

        logger.debug("URL: {}", uri.toString());
        HttpEntity<JSONObject> response = restTemplate.getForEntity(uri, JSONObject.class);

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            logger.warn("request was cached");
        } else {
            logger.info("request was actually executed");
        }

        JsonElement root = JsonParser.parseString(response.getBody().toString());
        
        JsonArray resources;
        try {
            resources = root.getAsJsonObject().get("Resources").getAsJsonArray();
		} catch (Exception e) {
			// the web service returns no array if the result is empty, this is a workaround to cover such situations
			if( root.getAsJsonObject().get("@confidence").getAsDouble() >= 0) {
				resources = new JsonArray(); 		
				logger.info("The web service response for '{}' was empty. However, the JSON response was valid.", myQuestion);
			} else {
				throw e;
			}
		}

        // check if anything was found
        if (resources.size() == 0) {
            logger.warn("nothing recognized for \"{}\": {}", myQuestion, response);
        }

        return resources;
    }
}
