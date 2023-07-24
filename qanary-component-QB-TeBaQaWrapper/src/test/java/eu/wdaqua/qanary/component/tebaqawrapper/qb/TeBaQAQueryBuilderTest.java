package eu.wdaqua.qanary.component.tebaqawrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.tebaqawrapper.qb.messages.TeBaQAResult;
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
class TeBaQAQueryBuilderTest {
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
     * @throws URISyntaxException
     */
    @Test
    @Disabled
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

}
