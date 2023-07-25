package eu.wdaqua.qanary.component.babelfy.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.babelfy.ned.exception.ApiTokenIsNullOrEmptyException;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

@Component
public class BabelfyServiceFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BabelfyServiceFetcher.class);

    private final String apiUrl;
    private final String apiToken;
    private final String params;
    private final float scoreThreshold;

    private RestTemplateWithCaching myRestTemplate;
    private CacheOfRestTemplateResponse myCacheOfResponses;

    public BabelfyServiceFetcher(
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Value("${babelfy.api.url}") String apiUrl, //
            @Value("${babelfy.api.key}") String apiToken, //
            @Value("${babelfy.api.parameters}") String params, //
            @Value("${babelfy.score.threshold}") String scoreThreshold
    ) throws ApiTokenIsNullOrEmptyException {
        if (apiToken == null || apiToken.isEmpty()) {
            throw new ApiTokenIsNullOrEmptyException();
        }

        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.params = params;
        this.scoreThreshold = Float.parseFloat(scoreThreshold);
    }

    public JsonArray sendRequestToApi(String myQuestion) throws URISyntaxException, UnsupportedEncodingException {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(this.apiUrl);
        uriBuilder.append("?text=").append(URLEncoder.encode(myQuestion, "UTF-8"));
        uriBuilder.append(this.params);
        uriBuilder.append("&key=").append(this.apiToken);

        URI uri = new URI(uriBuilder.toString());
        LOGGER.info("URI {}", uri);

        long requestBefore = this.myCacheOfResponses.getNumberOfExecutedRequests();

        ResponseEntity<String> response = this.myRestTemplate.getForEntity(uri, String.class);

        //TODO: check if response is valid
        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (this.myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", uri);
        } else {
            LOGGER.info("request was actually executed: {}", uri);
        }

        return JsonParser.parseString(response.getBody()).getAsJsonArray();
    }

    public ArrayList<Link> getLinksForQuestion(JsonArray jsonArray) {
        ArrayList<Link> links = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject explrObject = jsonArray.get(i).getAsJsonObject();
            LOGGER.info("JSON {}", explrObject);
            double score = explrObject.get("score").getAsDouble();
            if (score >= this.scoreThreshold) {
                JsonObject charArray = explrObject.get("charFragment").getAsJsonObject();
                int begin = charArray.get("start").getAsInt();
                int end = charArray.get("end").getAsInt();
                LOGGER.info("Begin: {}", begin);
                LOGGER.info("End: {}", end);

                Link l = new Link();
                l.begin = begin;
                l.end = end + 1;
                l.link = explrObject.get("DBpediaURL").getAsString();
                links.add(l);
            }
        }

        return links;
    }

    class Link {
        public int begin;
        public int end;
        public String link;
    }
}
