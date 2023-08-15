package eu.wdaqua.qanary.component.platypuswrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.DataNotProcessableException;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.PlatypusResult;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
//@WebAppConfiguration
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class PlatypusQueryBuilderTest {
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

}
