package eu.wdaqua.qanary.component.qanswer.qb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerResult;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerResult.QAnswerQueryCandidate;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class QAnswerQueryBuilderAndSpaqlResultFetcherTest {
    private static final Logger logger = LoggerFactory.getLogger(QAnswerQueryBuilderAndSpaqlResultFetcherTest.class);
    private final String applicationName = "QAnswerQueryBuilderAndExecutorTest";

    @Autowired
    private Environment env;

    @Autowired
    private RestTemplateWithCaching restTemplate;

    // TODO: replace by CachingRestTemplate when release in qanary.commons
    private RestTemplate restClient;

    private URI realEndpoint;

    @BeforeEach
    public void init() throws URISyntaxException {
        realEndpoint = new URI(env.getProperty("qanswer.endpoint.url"));

        assert (realEndpoint != null) : "qanswer.endpoint.url cannot be empty";

        // RestTemplateBuilder builder = new RestTemplateBuilder();
        this.restClient = new RestTemplate();
        assert this.restClient != null : "restclient cannot be null";
    }

    @Test
    void testComputeQuestionStringWithReplacedResourcesDBpedia() throws URISyntaxException {
        float threshold = 0.5f;
        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, "en", "dbpedia", "open",
                new URI("urn:no:endpoint"), applicationName, restTemplate);
        List<TestData> myTestData = new LinkedList<>();

        List<NamedEntity> entities0 = new LinkedList<>();
        URI parisUri = new URI("http://dbpedia.org/resource/Paris");
        URI londonUri = new URI("http://dbpedia.org/resource/London");
        URI germanyUri = new URI("http://dbpedia.org/resource/Germany");
        String givenQuestion = "Where are Paris, London and Germany?";
        String expectedResult = "Where are " + parisUri + " , " + londonUri + " and " + germanyUri + " ?";
        myTestData.add(new TestData(givenQuestion, expectedResult, entities0));
        entities0.add(new NamedEntity(parisUri, 10, "Paris", threshold));
        entities0.add(new NamedEntity(londonUri, 17, "London", threshold));
        entities0.add(new NamedEntity(germanyUri, 28, "Germany", threshold + 0.001f));

        // TODO: add more test data

        checkTestData(myTestData, threshold, myApp);
    }

    @Test
    void testComputeQuestionStringWithReplacedResourcesWikidata() throws URISyntaxException {
        float threshold = 0.5f;
        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, "en", "dbpedia", "open",
                new URI("urn:no:endpoint"), applicationName, restTemplate);
        List<TestData> myTestData = new LinkedList<>();

        List<NamedEntity> entities0 = new LinkedList<>();
        URI parisUri = getWikidataURI("90");
        URI londonUri = getWikidataURI("Q84");
        URI germanyUri = getWikidataURI("Q183");

        String givenQuestion = "Where are Paris, London and Germany?";
        String expectedResult = "Where are " + parisUri + " , " + londonUri + " and " + germanyUri + " ?";
        myTestData.add(new TestData(givenQuestion, expectedResult, entities0));
        entities0.add(new NamedEntity(parisUri, 10, "Paris", threshold));
        entities0.add(new NamedEntity(londonUri, 17, "London", threshold));
        entities0.add(new NamedEntity(germanyUri, 28, "Germany", threshold + 0.001f));

        // TODO: add more test data

        checkTestData(myTestData, threshold, myApp);
    }

    @Test
    void testThresholdBehavior() throws URISyntaxException {
        float threshold = 0.4f;
        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, "en", "wikidata", "open",
                new URI("urn:no:endpoint"), applicationName, restTemplate);
        List<TestData> myTestData = new LinkedList<>();

        List<NamedEntity> entities0 = new LinkedList<>();
        URI londonUri = new URI("http://dbpedia.org/resource/London");
        URI germanyUri = new URI("http://dbpedia.org/resource/Germany");
        myTestData.add(new TestData(//
                "Where are London and Germany?", //
                "Where are London and " + germanyUri.toString() + " ?", //
                entities0));
        entities0.add(new NamedEntity(londonUri, 10, "London", threshold - 0.001f));
        entities0.add(new NamedEntity(germanyUri, 21, "Germany", threshold));

        // TODO: add more test data

        checkTestData(myTestData, threshold, myApp);
    }

    private void checkTestData(List<TestData> myTestData, float threshold, QAnswerQueryBuilderAndSparqlResultFetcher myApp) {
        for (TestData t : myTestData) {
            String computedQuestion = myApp.computeQuestionStringWithReplacedResources(t.getQuestion(),
                    t.getNamedEntities(), threshold);
            logger.info("given question:  {}", t.getQuestion());
            logger.info("expected output: {}", t.getExpectedResult());
            logger.info("computed output: {}", computedQuestion);
            assertEquals(t.getExpectedResult(), computedQuestion);
        }
    }

    /**
     * creates a matching Wikidata entity URL from given ID, e.g., Q183 -->
     * http://www.wikidata.org/entity/Q183
     *
     * @param id
     * @return
     * @throws URISyntaxException
     */
    private URI getWikidataURI(String id) throws URISyntaxException {
        return new URI("http://www.wikidata.org/entity/" + id);
    }

    @Test
    @EnabledIf(
            expression = "#{environment['test.live.endpoints'] == 'true'}", //
            loadContext = true)    
    void testWebServiceWithOriginalQuestionString() throws URISyntaxException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "What is the capital of Germany?";

        // check if there is at least one result query candidate
        testWebService(myApp, question, lang, kb, user);
    }

    @Test
    @EnabledIf(
            expression = "#{environment['test.live.endpoints'] == 'true'}", //
            loadContext = true)    
    void testWebServiceWithReplacedResources() throws URISyntaxException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);

        // test with question enriched with a Wikidata entity
        URI franceUri = getWikidataURI("Q142");
        String qString = "Person born in " + franceUri.toString() + " .";
        // note: the functionality of computeQuestionStringWithReplacedResources() 
        //      is already tested in its own unit test, and can therefore be assumed!

        // check if there is at least one result query candidate
        testWebService(myApp, qString, lang, kb, user);

    }

    @Test
    @EnabledIf(
            expression = "#{environment['test.live.endpoints'] == 'true'}", //
            loadContext = true)    
    void testWebServiceResourceQuestionResultsInSparqlSelectQuery () throws MalformedURLException, URISyntaxException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "Is Berlin the capital of Germany";

        // check if the query candidates contain at least one ASK type query
        QAnswerResult result = testWebService(myApp, question, lang, kb, user);
        for (QAnswerQueryCandidate queryCandidate : result.getQueryCandidates()) {
            Query query = QueryFactory.create(queryCandidate.getQueryString());
            if (query.isSelectType()) {
                assert(true);
                return;
            } 
        }
        assert(false);
    }

    @Test
    @EnabledIf(
            expression = "#{environment['test.live.endpoints'] == 'true'}", //
            loadContext = true)    
    void testWebServiceBooleanQuestionResultsInSparqlAskQuery() throws MalformedURLException, URISyntaxException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "Is Berlin the capital of Germany";

        // check if the query candidates contain at least one ASK type query
        QAnswerResult result = testWebService(myApp, question, lang, kb, user);
        for (QAnswerQueryCandidate queryCandidate : result.getQueryCandidates()) {
            Query query = QueryFactory.create(queryCandidate.getQueryString());
            if (query.isAskType()) {
                assert(true);
                return;
            }
        }
        assert(false);
    }

    private QAnswerResult testWebService(QAnswerQueryBuilderAndSparqlResultFetcher myApp, String question, String lang, String kb, String user)
            throws URISyntaxException, MalformedURLException {
        QAnswerResult result = myApp.requestQAnswerWebService(realEndpoint, question, lang, kb, user);
        logger.debug("testWebService result: {}", result);
        assertTrue(result.getQueryCandidates().size() > 0);
        return result;
    }

}

class TestData {
    private String question;
    private List<NamedEntity> entities;
    private String expectedResult;

    public TestData(String questionGiven, String expectedResult, List<NamedEntity> entities) {
        this.question = questionGiven;
        this.expectedResult = expectedResult;
        this.entities = entities;
    }

    String getQuestion() {
        return this.question;
    }

    String getExpectedResult() {
        return this.expectedResult;
    }

    List<NamedEntity> getNamedEntities() {
        return this.entities;
    }
}
