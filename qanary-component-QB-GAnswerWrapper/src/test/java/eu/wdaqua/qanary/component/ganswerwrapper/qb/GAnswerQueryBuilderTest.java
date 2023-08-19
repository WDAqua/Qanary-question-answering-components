package eu.wdaqua.qanary.component.ganswerwrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.ganswerwrapper.qb.messages.GAnswerResult;
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
class GAnswerQueryBuilderTest {
    // time span for caching, tests wait this time span during the test runs
    protected final static int MAX_TIME_SPAN_SECONDS = 5;
    private static final Logger logger = LoggerFactory.getLogger(GAnswerQueryBuilderTest.class);
    private final String applicationName = "GAnswerQueryBuilder";
    private URI endpoint;
    @Autowired
    private Environment env;
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    @BeforeEach
    public void init() throws URISyntaxException {
        this.endpoint = new URI(env.getProperty("g_answer.endpoint.url"));
        logger.debug("endpoint: " + this.endpoint);

        assert (this.endpoint != null) : "g_answer.endpoint.url cannot be empty";
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

        GAnswerQueryBuilder gAnswerQueryBuilder = new GAnswerQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, this.myCacheOfResponse);

        assertTrue(gAnswerQueryBuilder.isLangSuppoerted("en"));
        assertTrue(gAnswerQueryBuilder.isLangSuppoerted("fr"));
        assertTrue(gAnswerQueryBuilder.isLangSuppoerted("es"));

        assertFalse(gAnswerQueryBuilder.isLangSuppoerted("ne"));
        assertFalse(gAnswerQueryBuilder.isLangSuppoerted("de"));
        assertFalse(gAnswerQueryBuilder.isLangSuppoerted("se"));
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

        GAnswerQueryBuilder teBaQAQueryBuilder = new GAnswerQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

//      TODO add question text
        String question = "";
        GAnswerResult result0 = testWebService(teBaQAQueryBuilder, question, langDefault);

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
    private GAnswerResult testWebService(GAnswerQueryBuilder myApp, String question, String lang) throws URISyntaxException {
        GAnswerResult result = myApp.requestGAnswerWebService(this.endpoint, question, lang);
        assertFalse(result.getSparql().isEmpty());
        return result;
    }

}
