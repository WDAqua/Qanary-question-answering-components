package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.dbpediaspotlight.ned.exceptions.DBpediaSpotlightJsonParsingNotPossible;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class DBpediaSpotlightServiceFetcherTest {
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcherTest.class);
	String knownValidResponseBody = "{\"types\":\"\",\"confidence\":\"0.1\",\"text\":\"test question Berlin London Tokio\",\"Resources\":{\"Resource\":[{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/Test_cricket\",\"support\":\"24138\",\"types\":\"\",\"surfaceForm\":\"test\",\"offset\":\"0\",\"similarityScore\":\"0.809517253401639\",\"percentageOfSecondRank\":\"0.0744734775216245\"},{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/Berlin\",\"support\":\"87107\",\"types\":\"Wikidata:Q515,Wikidata:Q486972,Schema:Place,Schema:City,DBpedia:Settlement,DBpedia:PopulatedPlace,DBpedia:Place,DBpedia:Location,DBpedia:City\",\"surfaceForm\":\"Berlin\",\"offset\":\"14\",\"similarityScore\":\"0.9993887385897859\",\"percentageOfSecondRank\":\"3.3106570817681E-4\"},{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/London\",\"support\":\"236613\",\"types\":\"Wikidata:Q515,Wikidata:Q486972,Schema:Place,Schema:City,DBpedia:Settlement,DBpedia:PopulatedPlace,DBpedia:Place,DBpedia:Location,DBpedia:City\",\"surfaceForm\":\"London\",\"offset\":\"21\",\"similarityScore\":\"0.9997648904521458\",\"percentageOfSecondRank\":\"7.949092738689299E-5\"},{\"URI\":\"http:\\/\\/dbpedia.org\\/resource\\/Tokyo\",\"support\":\"47818\",\"types\":\"Wikidata:Q515,Wikidata:Q486972,Schema:Place,Schema:City,DBpedia:Settlement,DBpedia:PopulatedPlace,DBpedia:Place,DBpedia:Location,DBpedia:City\",\"surfaceForm\":\"Tokio\",\"offset\":\"28\",\"similarityScore\":\"0.9983547399586975\",\"percentageOfSecondRank\":\"0.0015053572463341422\"}]},\"sparql\":\"\",\"support\":\"0\",\"policy\":\"whitelist\"}";
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    @Value("${spring.application.name}")
    private String applicationName;

    private DBpediaSpotlightServiceFetcher mockedDBpediaSpotlightServiceFetcher;
    private QanaryQuestion mockedQanaryQuestion;
    private DBpediaSpotlightNED mockedDBpediaSpotlightNED;

    static {
    	// deactivate the live test of the real-world webservice
        System.setProperty("dbpediaspotlight.perform-live-check-on-component-start", "false");
        System.setProperty("dbpediaspotlight.endpoint.ssl.certificatevalidation.ignore", "false");
    }
    
    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, DBpediaSpotlightJsonParsingNotPossible, IOException {
        assert this.restTemplate != null : "restTemplate cannot be null";

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));

        this.mockedDBpediaSpotlightServiceFetcher = Mockito.mock(DBpediaSpotlightServiceFetcher.class, Mockito.RETURNS_DEEP_STUBS);

        this.mockedDBpediaSpotlightNED = Mockito.mock(DBpediaSpotlightNED.class);
        Mockito.when(this.mockedDBpediaSpotlightNED.getSparqlInsertQuery(any(FoundDBpediaResource.class), any(QanaryQuestion.class))).thenCallRealMethod();
    }
    
    @Test
    void testParsingOfJsonResponseOffline() throws ParseException, DBpediaSpotlightJsonParsingNotPossible {
    	//DBpediaSpotlightServiceFetcher myFetcher = new DBpediaSpotlightServiceFetcher();
      Mockito.when(this.mockedDBpediaSpotlightServiceFetcher.getResourcesOfResponse(any(HttpEntity.class), any(String.class))).thenCallRealMethod();
      Mockito.when(this.mockedDBpediaSpotlightServiceFetcher.parseJsonBodyOfResponse(any(HttpEntity.class))).thenCallRealMethod();
    	JSONParser parser = new JSONParser();
    	
    	JSONObject body = (JSONObject) parser.parse(knownValidResponseBody);
		HttpEntity<JSONObject> response = new HttpEntity<JSONObject>(body);
    logger.info(response.toString());
		JsonArray resources = this.mockedDBpediaSpotlightServiceFetcher.getResourcesOfResponse(response, knownValidResponseBody);
    logger.info(resources.toString());
		
		assertEquals(4, resources.size());
		
		for (int i = 0; i < resources.size(); i++) {
			JsonElement resource = resources.get(i);
			assertNotEquals(null, resource, "was null: " + resource.toString());
		}
    }
    
    @Test
    void testFoundResources() throws ParseException, DBpediaSpotlightJsonParsingNotPossible, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, IOException {
        JSONParser parser = new JSONParser();
        JSONObject body = (JSONObject) parser.parse(knownValidResponseBody);
        HttpEntity<JSONObject> response = new HttpEntity<JSONObject>(body);
        JsonArray resources = this.mockedDBpediaSpotlightServiceFetcher.getResourcesOfResponse(response, knownValidResponseBody);

        List<FoundDBpediaResource> foundDBpediaResources = this.mockedDBpediaSpotlightServiceFetcher.getListOfResources(resources);
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

    @Test
    void testGetSparqlInsertQuery() throws ParseException, DBpediaSpotlightJsonParsingNotPossible, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, IOException {
        JSONParser parser = new JSONParser();
        JSONObject body = (JSONObject) parser.parse(knownValidResponseBody);
        HttpEntity<JSONObject> response = new HttpEntity<JSONObject>(body);
        JsonArray resources = this.mockedDBpediaSpotlightServiceFetcher.getResourcesOfResponse(response, knownValidResponseBody);
        List<FoundDBpediaResource> foundDBpediaResources = this.mockedDBpediaSpotlightServiceFetcher.getListOfResources(resources);
        for (FoundDBpediaResource foundDBpediaResource : foundDBpediaResources) {
            String sparql = this.mockedDBpediaSpotlightNED.getSparqlInsertQuery(foundDBpediaResource, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }
}
