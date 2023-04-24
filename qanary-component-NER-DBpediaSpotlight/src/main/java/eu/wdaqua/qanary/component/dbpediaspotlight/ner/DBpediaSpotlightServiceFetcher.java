package eu.wdaqua.qanary.component.dbpediaspotlight.ner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.dbpediaspotlight.ner.exceptions.DBpediaSpotlightJsonParsingNotPossible;;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class DBpediaSpotlightServiceFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcher.class);

    /**
     * fetch data from the configured DBpedia Spotlight endpoint
     *
     * @param myQuestion
     * @param endpoint
     * @param minimumConfidence
     * @param restTemplate
     * @param myCacheOfResponses
     * @return
     * @throws URISyntaxException
     * @throws DBpediaSpotlightJsonParsingNotPossible
     */
    public JsonArray getJsonFromService(
            String myQuestion, //
            String endpoint, //
            float minimumConfidence, //
            RestTemplate restTemplate, //
            CacheOfRestTemplateResponse myCacheOfResponses //
    ) throws URISyntaxException, DBpediaSpotlightJsonParsingNotPossible {
        URI uri = createRequestUriWithParameters(myQuestion, endpoint, minimumConfidence);
        JsonObject response = fetchNamedEntitiesFromWebService(restTemplate, myCacheOfResponses, uri);
        JsonArray resources = getResourcesOfResponse(response, myQuestion);

        return resources;
    }

    public URI createRequestUriWithParameters(
            String myQuestion, //
            String endpoint, //
            float minimumConfidence //
    ) throws URISyntaxException {

        StringBuilder uriGetParameter = new StringBuilder();
        uriGetParameter.append(endpoint);
        uriGetParameter.append("?text=").append(URLEncoder.encode(myQuestion, StandardCharsets.UTF_8));
        uriGetParameter.append("&confidence=").append(minimumConfidence);

        URI uri = new URI(uriGetParameter.toString());
        LOGGER.debug("URL: {}", uri);
        return uri;
    }

    public JsonObject fetchNamedEntitiesFromWebService(
            RestTemplate restTemplate, //
            CacheOfRestTemplateResponse myCacheOfResponses, //
            URI uri //
    ) {
        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();
        HttpEntity<String> response = restTemplate.getForEntity(uri, String.class);

        // show caching status of current request
        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached");
        } else {
            LOGGER.info("request was actually executed");
        }

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        JsonObject responseJson = JsonParser.parseString(response.getBody()).getAsJsonObject();

        return responseJson;
    }

    public JsonArray getResourcesOfResponse(JsonObject response, String myQuestion)
            throws DBpediaSpotlightJsonParsingNotPossible {
        JsonArray resources;

        try {
            // until 2022-11
            // resources = root.getAsJsonObject().get("Resources").getAsJsonArray();
            // from 2022-12
            resources = response.get("Resources").getAsJsonObject().get("Resource").getAsJsonArray();
        } catch (Exception e) {
            // the web service returns no array if the result is empty, this is a workaround
            // to cover such situations
            if (response.getAsJsonObject().get("confidence").getAsDouble() >= 0) {
                resources = new JsonArray();
                LOGGER.info("The web service response for '{}' was empty. However, the JSON response was valid.",
                        myQuestion);
            } else {
                LOGGER.error("Parsing of service response failed while retrieving Resources.Resource: {}\n{}",
                        e, response);
                throw new DBpediaSpotlightJsonParsingNotPossible(e + " regarding " + response);
            }
        }

        // check if anything was found
        if (resources.size() == 0) {
            LOGGER.warn("nothing recognized for \"{}\": {}", myQuestion, response);
        } else {
            LOGGER.warn("{} resources recognized for \"{}\": {}", resources.size(), myQuestion, response);
        }

        return resources;
    }

    public List<FoundDBpediaResource> getListOfResources(JsonArray resources) throws URISyntaxException {
        List<FoundDBpediaResource> foundDBpediaResources = new LinkedList<>();
        for (int i = 0; i < resources.size(); i++) {
            foundDBpediaResources.add(new FoundDBpediaResource(resources.get(i)));
            LOGGER.debug("found resource ({} of {}): {} at ({},{})", //
                    i, resources.size() - 1, //
                    foundDBpediaResources.get(i).getResource(), //
                    foundDBpediaResources.get(i).getBegin(), //
                    foundDBpediaResources.get(i).getEnd() //
            );
        }
        return foundDBpediaResources;
    }
}
