package eu.wdaqua.qanary.tebaqa.wrapper;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.tebaqa.wrapper.messages.TeBaQAResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TeBaQAQueryBuilderTest {
    // time span for caching, tests wait this time span during the test runs
    protected final static int MAX_TIME_SPAN_SECONDS = 5;
    private static final Logger logger = LoggerFactory.getLogger(TeBaQAQueryBuilderTest.class);
    private final String applicationName = "TeBaQAQueryBuilderTest";
    private URI endpoint;
    @Autowired
    private Environment env;
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    @BeforeEach
    public void init() throws URISyntaxException {
        this.endpoint = new URI(env.getProperty("tebaqa.endpoint.url"));
        logger.debug("endpoint: " + this.endpoint);

        assert (this.endpoint != null) : "tebaqa.endpoint.url cannot be empty";
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
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

        TeBaQAQueryBuilder teBaQAQueryBuilder = new TeBaQAQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        assertTrue(teBaQAQueryBuilder.isLangSupported("en"));

        assertFalse(teBaQAQueryBuilder.isLangSupported("fr"));
        assertFalse(teBaQAQueryBuilder.isLangSupported("es"));
        assertFalse(teBaQAQueryBuilder.isLangSupported("ne"));
        assertFalse(teBaQAQueryBuilder.isLangSupported("de"));
        assertFalse(teBaQAQueryBuilder.isLangSupported("se"));
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
        TimeUnit.SECONDS.sleep(MAX_TIME_SPAN_SECONDS + 1); // wait until it is too late for caching
        callRestTemplateWithCaching(loginForm0, Cache.NOT_CACHED); // cache miss: too long ago
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm1, Cache.NOT_CACHED); // cache miss: different body
        callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(loginForm1, Cache.CACHED); // cache hit

        assertEquals(initialNumberOfRequests + 3, myCacheOfResponse.getNumberOfExecutedRequests());

    }

    /**
     * @throws URISyntaxException
     */
    @Test
    void liveTest() throws URISyntaxException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

        TeBaQAQueryBuilder teBaQAQueryBuilder = new TeBaQAQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        String question = "How many awards has Bertrand Russell?";
        TeBaQAResult result0 = testWebService(teBaQAQueryBuilder, question, langDefault);

        String expectedSparql = "SELECT (COUNT(?uri) AS ?count) WHERE { <http://dbpedia.org/resource/Bertrand_Russell> <http://dbpedia.org/property/awards> ?uri }";

        assertEquals(result0.getSparql(), expectedSparql);
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
     */
    private TeBaQAResult testWebService(TeBaQAQueryBuilder myApp, String question, String lang) throws URISyntaxException {
        TeBaQAResult result = myApp.requestTeBaQAWebService(this.endpoint, question, lang);
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
