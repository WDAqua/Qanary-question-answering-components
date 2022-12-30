package eu.wdaqua.qanary.component.qanswer.qbe;

import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.NoLiteralFieldFoundException;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.QAnswerResult;
import net.minidev.json.parser.ParseException;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class QAnswerQueryBuilderAndExecutorTest {
    private static final Logger logger = LoggerFactory.getLogger(QAnswerQueryBuilderAndExecutorTest.class);
    private final String applicationName = "QAnswerQueryBuilderAndExecutorTest";

    @Autowired
    private Environment env;

    @Autowired
    private RestTemplateWithCaching restTemplate;

    // TODO: replace by CachingRestTemplate when release in qanary.commons
    private RestTemplate restClient;

    private URI realEndpoint;

    private URI resource;
    private URI bool;
    private URI decimal;

    @BeforeEach
    public void init() throws URISyntaxException {
        realEndpoint = new URI(env.getProperty("qanswer.endpoint.url"));

        bool = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        decimal = new URI("http://www.w3.org/2001/XMLSchema#decimal");
        resource = new URI("http://www.w3.org/2001/XMLSchema#anyURI");

        assert (realEndpoint != null) : "qanswer.endpoint.url cannot be empty";

        // RestTemplateBuilder builder = new RestTemplateBuilder();
        this.restClient = new RestTemplate();
        assert this.restClient != null : "restclient cannot be null";
    }

    @Test
    void testTransformationOfNamedEntites() throws URISyntaxException, MalformedURLException {
        float threshold = 0.5f;
        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, "en", "dbpedia",
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
    void testThresholdBehavior() throws URISyntaxException, MalformedURLException {
        float threshold = 0.4f;
        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, "en", "wikidata",
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

    private void checkTestData(List<TestData> myTestData, float threshold, QAnswerQueryBuilderAndExecutor myApp) {
        for (TestData t : myTestData) {
            String computedQuestion = myApp.computeQuestionStringWithReplacedResources(t.getQuestion(),
                    t.getNamedEntities(), threshold);
            logger.info("given question:  {}", t.getQuestion());
            logger.info("expexted output: {}", t.getExpectedResult());
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

    /**
     * test actual results from the QAnswer API with question 'What is the capital
     * of Germany?' --> one resource
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServiceWhatIsTheCapitalOfGermanyResultOneResource() throws URISyntaxException, ParseException, NoLiteralFieldFoundException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";

        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, lang, kb,
                this.realEndpoint, applicationName, restTemplate);
        String question = "What is the capital of Germany?";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb);

        URI germanyUri = getWikidataURI("Q183");
        String expectedQuestion = "What is the capital of " + germanyUri.toString() + " ?";

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(germanyUri, 23, "Germany", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);

        // check correct transformation of the given question
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        //
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb);

        logger.warn("results: {} of {}  vs.  {} of {}", result0.getValues().size(), result0.getType(),
                result1.getValues().size(), result1.getType());
        assertEquals(result0.getValues().size(), result1.getValues().size(), "size of results has to be equal");
        assertEquals(result0.getType(), result1.getType(), "type of results has to be equal");

        assertEquals("uri", result1.getType());
        assertEquals(result0.getType(), result1.getType());

        assertEquals(resource, result1.getDatatype());
        assertEquals(result0.getDatatype(), result1.getDatatype());
    }

    /**
     * test actual results from the QAnswer API with question 'Cities in France?'
     * --> many resources
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServicePersonBornInFranceResultManyResources() throws URISyntaxException, ParseException, NoLiteralFieldFoundException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        int min = 2;
        int max = 1000;

        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, lang, kb,
                this.realEndpoint, applicationName, restTemplate);
        String question = "Person born in France.";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb);
        assertTrue(result0.getValues().size() >= min, "problem: not " + result0.getValues().size() + " >= " + min);
        assertTrue(result0.getValues().size() <= max, "problem: not " + result0.getValues().size() + " <= " + max);
        assertEquals("uri", result0.getType());

        URI franceUri = getWikidataURI("Q142");
        String expectedQuestion = "Person born in " + franceUri.toString() + " .";

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(franceUri, 15, "France", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);

        // check correct transformation of the given question
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        // Note: we do not know the exact number of cities in France provided by QAnswer
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb);

        assertTrue(result1.getValues().size() >= min, "problem: not " + result1.getValues().size() + " >= " + min);
        assertTrue(result1.getValues().size() <= max, "problem: not " + result1.getValues().size() + " <= " + max);

        assertEquals("uri", result1.getType());
        assertEquals(result0.getType(), result1.getType());

        assertEquals(resource, result1.getDatatype());
        assertEquals(result0.getDatatype(), result1.getDatatype());

        // DOES NOT WORK ALWAYS
        // logger.warn("results: {} of {} vs. {} of {}", result0.getValues().size(),
        // result0.getType(), result1.getValues().size(), result1.getType() );
        // assertEquals("size of results has to be equal", result0.getValues().size(),
        // result1.getValues().size());
        // assertEquals("type of results has to be equal", result0.getType(),
        // result1.getType());
    }

    /**
     * test actual results from the QAnswer API with question 'Is Berlin the capital
     * of Germany' --> many resources
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServiceIsBerlinTheCapitalOfGermanyResultBoolean() throws URISyntaxException, ParseException, NoLiteralFieldFoundException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";

        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, lang, kb,
                this.realEndpoint, applicationName, restTemplate);
        String question = "Is Berlin the capital of Germany";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb);

        URI berlinUri = getWikidataURI("Q64");
        String expectedQuestion = "Is " + berlinUri.toString() + " the capital of Germany";

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(berlinUri, 3, "Berlin", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);
        // computedQuestion = "http://dbpedia.org/resource/United_Kingdom
        // http://dbpedia.org/ontology/capital http://dbpedia.org/resource/London";

        // check correct transformation of the given question
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        // receive a boolean answer
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb);

        String errorMessage = "'" + question + "' -> " + result0.getType() + "  =!=  '" + computedQuestion + "' -> "
                + result1.getType();
        assertEquals(result0.getType(), result1.getType(), errorMessage);
        assertEquals(result0.getType(), result1.getType());

        assertEquals(result0.getValues().get(0), result1.getValues().get(0));

        assertEquals(bool, result0.getDatatype());
        assertEquals(result0.getDatatype(), result1.getDatatype());

    }

    /**
     * test actual results from the QAnswer API with question 'What is the capital
     * of Germany?' --> one resource
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServicePopulationOfFranceResultNumber() throws URISyntaxException, ParseException, NoLiteralFieldFoundException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";

        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, lang, kb,
                this.realEndpoint, applicationName, restTemplate);
        String question = "population of france";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb);

        URI everestUri = getWikidataURI("Q142");
        String expectedQuestion = "population of " + everestUri.toString();

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(everestUri, "population of ".length(), "france", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);

        // check correct transformation of the given question
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        // receive a boolean answer
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb);

        String errorMessage = "'" + question + "' -> " + result0.getType() + "  =!=  '" + computedQuestion + "' -> "
                + result1.getType();
        assertEquals(result0.getType(), result1.getType(), errorMessage);
        assertEquals(result0.getValues().get(0), result1.getValues().get(0), "Results are not equal");

        assertEquals(decimal, result0.getDatatype(), "Datatype should be a '" + decimal + "', but was " + result0.getDatatype());
        assertEquals(result0.getDatatype(), result1.getDatatype());
        assertEquals(result0.getValues().get(0), result1.getValues().get(0));
    }

    /**
     * check if string is computed
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServiceWhatIsTheNicknameOfRomeResultString() throws URISyntaxException, ParseException, NoLiteralFieldFoundException, MalformedURLException {

        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";

        QAnswerQueryBuilderAndExecutor myApp = new QAnswerQueryBuilderAndExecutor(threshold, lang, kb,
                this.realEndpoint, applicationName, restTemplate);
        String question = "what is the nickname of Rome";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb);

        assertEquals("literal", result0.getType());
        assertEquals(result0.STRINGTYPEURI, result0.getDatatype());
        assertTrue(result0.getValues().size() > 2);
    }


    private QAnswerResult testWebService(QAnswerQueryBuilderAndExecutor myApp, String question, String lang, String kb)
            throws URISyntaxException, NoLiteralFieldFoundException {
        QAnswerResult result = myApp.requestQAnswerWebService(realEndpoint, question, lang, kb);
        assertFalse(result.getType().isEmpty());
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
