package eu.wdaqua.qanary.component.platypuswrapper.qb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.DataNotProcessableException;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.PlatypusResult;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
//@WebAppConfiguration
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class PlatypusQueryBuilderTest {
    // time span for caching, tests wait this time span during the test runs
    protected final static int MAX_TIME_SPAN_SECONDS = 5;
    private static final Logger logger = LoggerFactory.getLogger(PlatypusQueryBuilderTest.class);
    private final String applicationName = "PlatypusQueryBuilder";
    private URI endpoint;
    @Autowired
    private Environment env;
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    private URI resoureceUri;
    private URI booleanUri;
    private URI stringUri;
    private URI dateUri;
    private URI floatUri;
    private URI integerUri;

    @BeforeEach
    public void init() throws URISyntaxException {
        this.endpoint = new URI(env.getProperty("platypus.endpoint.url"));
        logger.debug("endpoint: " + this.endpoint);

        resoureceUri = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        booleanUri = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        stringUri = new URI("http://www.w3.org/2001/XMLSchema#string");
        dateUri = new URI("http://www.w3.org/2001/XMLSchema#date");
        floatUri = new URI("http://www.w3.org/2001/XMLSchema#float");
        integerUri = new URI("http://www.w3.org/2001/XMLSchema#integer");

        assert (this.endpoint != null) : "platypus.endpoint.url cannot be empty";
        assert this.restTemplate != null : "restTemplate cannot be null";
    }

    /**
     * test supported Languages
     *
     * @throws Exception
     */
    @Test
    void testIsLangSuppoerted() throws Exception {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en", "fr", "es"));

        PlatypusQueryBuilder platypusQueryBuilder = new PlatypusQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, this.myCacheOfResponse);

        assertTrue(platypusQueryBuilder.isLangSupported("en"));
        assertTrue(platypusQueryBuilder.isLangSupported("fr"));
        assertTrue(platypusQueryBuilder.isLangSupported("es"));

        assertFalse(platypusQueryBuilder.isLangSupported("ne"));
        assertFalse(platypusQueryBuilder.isLangSupported("de"));
        assertFalse(platypusQueryBuilder.isLangSupported("se"));
    }

    /**
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    @EnabledIf(expression = "#{environment['platypus.api.live.test.active'] == 'true'}", loadContext = true)
    void givenRestTemplate_whenRequested_thenLogAndModifyResponse() throws InterruptedException, URISyntaxException {

        assertNotNull(restTemplate);
        assertNotNull(myCacheOfResponse);

        LoginForm loginForm0 = new LoginForm("userName", "password");
        LoginForm loginForm1 = new LoginForm("userName2", "password2");

        long initialNumberOfRequests = myCacheOfResponse.getNumberOfExecutedRequests();

        callRestTemplateWithCaching(loginForm0, Cache.NOT_CACHED); // cache miss
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        TimeUnit.SECONDS.sleep(MAX_TIME_SPAN_SECONDS + 1); // wait until it is too late for caching
        callRestTemplateWithCaching(loginForm0, Cache.NOT_CACHED); // cache miss: too long ago
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm1, Cache.NOT_CACHED); // cache miss: different body
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm1, Cache.CACHED); // cache hit

        assertEquals(initialNumberOfRequests + 3, myCacheOfResponse.getNumberOfExecutedRequests());

    }

    /**
     * Test answer with date literal 
     * @throws URISyntaxException
     * @throws DataNotProcessableException
     */
    @Test
    @EnabledIf(expression = "#{environment['platypus.api.live.test.active'] == 'true'}", loadContext = true)
    void testWebServiceWhenWasAngelaMerkelBornResultOneDate() throws URISyntaxException, DataNotProcessableException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en", "fr", "es"));

        PlatypusQueryBuilder platypusQueryBuilder = new PlatypusQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        String question = "When was Angela Merkel born?";
        PlatypusResult result0 = testWebService(platypusQueryBuilder, question, langDefault);

        String expectedSparql = "SELECT DISTINCT ?result3 WHERE {\n\twd:Q567 wdt:P569 ?result3 .\n} LIMIT 100";
        List<String> expectedValues = Arrays.asList("1954-07-17Z");
        URI expectedType = dateUri;

        assertEquals(expectedSparql, result0.getSparql());
        assertEquals(expectedValues, result0.getValues());
        assert(result0.getValues().size() == 1);
        assertEquals(expectedType, result0.getDatatype());

    }
    
    // TODO: test answer with boolean 
    // TODO: test answer with string literal
    // TODO: test answer with float literal
    // TODO: test answer with integer literal

    /**
     * Test answer with resource
     *
     * capital of Germany usually returns two resources (Berlin and Bonn), 
     * consider a different question
     *
     * @throws URISyntaxException
     * @throws DataNotProcessableException
     */
    @Test
    @EnabledIf(expression = "#{environment['platypus.api.live.test.active'] == 'true'}", loadContext = true)
    void testWebServiceWhatIsTheCapitalOfGermanyResultAtLeastOneResource() throws URISyntaxException, DataNotProcessableException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en", "fr", "es"));

        PlatypusQueryBuilder platypusQueryBuilder = new PlatypusQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        String question = "What is the capital of Germany?";
        PlatypusResult result0 = testWebService(platypusQueryBuilder, question, langDefault);

        String expectedSparql = "SELECT DISTINCT ?result2 WHERE {\n\t?result2 wdt:P1376 wd:Q183 .\n} LIMIT 100";
        String expectedValue = "wd:Q64";
        URI expectedType = resoureceUri;

        assertEquals(expectedSparql, result0.getSparql());
        assert(result0.getValues().contains(expectedValue));
        assert(result0.getValues().size() > 0);
        assertEquals(expectedType, result0.getDatatype());
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

    /**
     * @param myApp
     * @param question
     * @param lang
     * @return
     * @throws URISyntaxException
     * @throws DataNotProcessableException
     */
    private PlatypusResult testWebService(PlatypusQueryBuilder myApp, String question, String lang) throws URISyntaxException, DataNotProcessableException {
        PlatypusResult result = myApp.requestPlatypusWebService(this.endpoint, question, lang);
        assertFalse(result.getSparql().isEmpty());
        return result;
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

}
