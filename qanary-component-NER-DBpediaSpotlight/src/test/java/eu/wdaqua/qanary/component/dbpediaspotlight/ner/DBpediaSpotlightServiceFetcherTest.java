package eu.wdaqua.qanary.component.dbpediaspotlight.ner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.dbpediaspotlight.ner.exceptions.DBpediaSpotlightJsonParsingNotPossible;
import jakarta.json.Json;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class DBpediaSpotlightServiceFetcherTest {
    // time span for caching, tests wait this time span during the test runs
    protected final static int MAX_TIME_SPAN_SECONDS = 30;
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcherTest.class);
    String knownValidResponseBody = "{\"types\":\"\",\"confidence\":\"0.1\",\"text\":\"test question Berlin London Tokio\",\"Resources\":{\"Resource\":[{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/Test_cricket\",\"support\":\"24138\",\"types\":\"\",\"surfaceForm\":\"test\",\"offset\":\"0\",\"similarityScore\":\"0.809517253401639\",\"percentageOfSecondRank\":\"0.0744734775216245\"},{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/Berlin\",\"support\":\"87107\",\"types\":\"Wikidata:Q515,Wikidata:Q486972,Schema:Place,Schema:City,DBpedia:Settlement,DBpedia:PopulatedPlace,DBpedia:Place,DBpedia:Location,DBpedia:City\",\"surfaceForm\":\"Berlin\",\"offset\":\"14\",\"similarityScore\":\"0.9993887385897859\",\"percentageOfSecondRank\":\"3.3106570817681E-4\"},{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/London\",\"support\":\"236613\",\"types\":\"Wikidata:Q515,Wikidata:Q486972,Schema:Place,Schema:City,DBpedia:Settlement,DBpedia:PopulatedPlace,DBpedia:Place,DBpedia:Location,DBpedia:City\",\"surfaceForm\":\"London\",\"offset\":\"21\",\"similarityScore\":\"0.9997648904521458\",\"percentageOfSecondRank\":\"7.949092738689299E-5\"},{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/Tokyo\",\"support\":\"47818\",\"types\":\"Wikidata:Q515,Wikidata:Q486972,Schema:Place,Schema:City,DBpedia:Settlement,DBpedia:PopulatedPlace,DBpedia:Place,DBpedia:Location,DBpedia:City\",\"surfaceForm\":\"Tokio\",\"offset\":\"28\",\"similarityScore\":\"0.9983547399586975\",\"percentageOfSecondRank\":\"0.0015053572463341422\"}]},\"sparql\":\"\",\"support\":\"0\",\"policy\":\"whitelist\"}";
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    static {
        // deactivate the live test of the real-world webservice
        System.setProperty("dbpediaspotlight.perform-live-check-on-component-start", "false");
        System.setProperty("dbpediaspotlight.endpoint.ssl.certificatevalidation.ignore", "false");
    }

    @BeforeEach
    public void init() throws URISyntaxException {
        assert this.restTemplate != null : "restTemplate cannot be null";
    }

    /**
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    void givenRestTemplate_whenRequested_thenLogAndModifyResponse() throws InterruptedException, URISyntaxException {

        assertNotNull(restTemplate);
        assertNotNull(myCacheOfResponse);

        LoginForm loginForm0 = new LoginForm("userName", "password");
        LoginForm loginForm1 = new LoginForm("userName2", "password2");

        long initialNumberOfRequests = myCacheOfResponse.getNumberOfExecutedRequests();

        callRestTemplateWithCaching(loginForm0, Cache.NOT_CACHED); // cache miss
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        TimeUnit.SECONDS.sleep(MAX_TIME_SPAN_SECONDS + 10); // wait until it is too late for caching
        callRestTemplateWithCaching(loginForm0, Cache.NOT_CACHED); // cache miss: too long ago
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm1, Cache.NOT_CACHED); // cache miss: different body
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm1, Cache.CACHED); // cache hit

        assertEquals(initialNumberOfRequests + 3, myCacheOfResponse.getNumberOfExecutedRequests());

    }

    /**
     * @param loginForm
     * @param cacheStatus
     * @throws URISyntaxException
     */
    private void callRestTemplateWithCaching(LoginForm loginForm, Cache cacheStatus) throws URISyntaxException {
        URI TESTSERVICEURL = new URI("http://httpbin.org/post");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginForm> requestEntity = new HttpEntity<LoginForm>(loginForm, headers);

        long numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests();
        logger.debug("execute POST request on {}", TESTSERVICEURL);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(TESTSERVICEURL, requestEntity, String.class);
        numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests() - numberOfNewlyExecutedRequests;
        logger.info("numberOfExecutedRequest since last request: new={}, count={}, teststatus={}", //
                numberOfNewlyExecutedRequests, myCacheOfResponse.getNumberOfExecutedRequests(), cacheStatus);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        switch (cacheStatus) {
            case NOT_CACHED:
                assertEquals(1, numberOfNewlyExecutedRequests);
                break;
            case CACHED:
                assertEquals(0, numberOfNewlyExecutedRequests);
                break;
            default:
                fail("Test case misconfigured");
                break;
        }
    }

    private enum Cache {
        CACHED, NOT_CACHED
    }

    public class LoginForm {
        private String username;
        private String password;

        public LoginForm() {
        }

        public LoginForm(String username, String password) {
            super();
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Test
    void testParsingOfJsonResponseOffline() throws ParseException, DBpediaSpotlightJsonParsingNotPossible {
        DBpediaSpotlightServiceFetcher myFetcher = new DBpediaSpotlightServiceFetcher();
        JSONParser parser = new JSONParser();
        JsonElement bodyElement = JsonParser.parseString(knownValidResponseBody);
        JsonObject response = bodyElement.getAsJsonObject();
        JsonArray resources = myFetcher.getResourcesOfResponse(response, knownValidResponseBody);

        assertEquals(4, resources.size());

        for (int i = 0; i < resources.size(); i++) {
            JsonElement resource = resources.get(i);
            assertNotEquals(null, resource, "was null: " + resource.toString());
        }
    }

    @Test
    void testFoundResources() throws DBpediaSpotlightJsonParsingNotPossible, URISyntaxException {
        DBpediaSpotlightServiceFetcher myFetcher = new DBpediaSpotlightServiceFetcher();
        JsonElement bodyElement = JsonParser.parseString(knownValidResponseBody);
        JsonObject response = bodyElement.getAsJsonObject();
        JsonArray resources = myFetcher.getResourcesOfResponse(response, knownValidResponseBody);
        List<FoundDBpediaResource> foundDBpediaResources = myFetcher.getListOfResources(resources);
        for (FoundDBpediaResource foundDBpediaResource : foundDBpediaResources) {
            assertNotEquals(null, foundDBpediaResource);
            assertTrue(foundDBpediaResource.getBegin() >= 0);
            assertTrue(foundDBpediaResource.getEnd() >= 0);
            assertTrue(foundDBpediaResource.getEnd() >= foundDBpediaResource.getBegin());
            assertTrue(foundDBpediaResource.getSimilarityScore() > 0);
            assertTrue(foundDBpediaResource.getSupport() >= 0);
            assertNotEquals(null, foundDBpediaResource.getResource());
        }
    }

}
