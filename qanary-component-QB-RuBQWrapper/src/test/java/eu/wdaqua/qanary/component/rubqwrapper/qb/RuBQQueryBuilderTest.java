package eu.wdaqua.qanary.component.rubqwrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.rubqwrapper.qb.messages.RuBQResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class RuBQQueryBuilderTest {
    // time span for caching, tests wait this time span during the test runs
    protected final static int MAX_TIME_SPAN_SECONDS = 5;
    private static final Logger logger = LoggerFactory.getLogger(RuBQQueryBuilderTest.class);
    private final String applicationName = "RuBQQueryBuilder";
    private URI endpoint;
    @Autowired
    private Environment env;
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    @BeforeEach
    public void init() throws URISyntaxException {
        this.endpoint = new URI(env.getProperty("rubq.endpoint.url"));
        logger.debug("endpoint: " + this.endpoint);

        assert (this.endpoint != null) : "rubq.endpoint.url cannot be empty";
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

        RuBQQueryBuilder ruBQQueryBuilder = new RuBQQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        assertTrue(ruBQQueryBuilder.isLangSuppoerted("en"));
        assertTrue(ruBQQueryBuilder.isLangSuppoerted("fr"));
        assertTrue(ruBQQueryBuilder.isLangSuppoerted("es"));

        assertFalse(ruBQQueryBuilder.isLangSuppoerted("ne"));
        assertFalse(ruBQQueryBuilder.isLangSuppoerted("de"));
        assertFalse(ruBQQueryBuilder.isLangSuppoerted("se"));
    }

    /**
     * @throws URISyntaxException
     */
    @Test
    @Disabled
//  TODO add live URL
    void liveTest() throws URISyntaxException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en", "fr", "es"));

        RuBQQueryBuilder teBaQAQueryBuilder = new RuBQQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

//      TODO add question text
        String question = "";
        RuBQResult result0 = testWebService(teBaQAQueryBuilder, question, langDefault);

//      TODO add expected result
        String expectedSparql = "";

        assertEquals(result0.getSparql(), expectedSparql);
    }

    /**
     * @param myApp
     * @param question
     * @param lang
     * @return
     * @throws URISyntaxException
     */
    private RuBQResult testWebService(RuBQQueryBuilder myApp, String question, String lang) throws URISyntaxException {
        RuBQResult result = myApp.requestRuBQWebService(this.endpoint, question, lang);
        assertFalse(result.getSparql().isEmpty());
        return result;
    }

}
