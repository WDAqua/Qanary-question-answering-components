package eu.wdaqua.qanary.rubq_wrapper;

import eu.wdaqua.qanary.rubq_wrapper.messages.RuBQResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestRuBQQueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TestRuBQQueryBuilder.class);
    private final String applicationName = "PlatypusQueryBuilder";

    @Autowired
    private Environment env;

    URI endpoint;
    RestTemplate restTemplate;


    @Before
    public void init() throws URISyntaxException {

        this.endpoint = new URI(env.getProperty("rubq.endpoint.url"));
        assert (this.endpoint != null) : "rubq.endpoint.url cannot be empty";

        this.restTemplate = new RestTemplate();
        assert this.restTemplate != null : "restTemplate cannot be null";
    }

    /**
     * test supported Languages
     *
     * @throws Exception
     */
    @Test
    public void testIsLangSuppoerted() throws Exception {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en", "fr", "es"));

        RuBQQueryBuilder ruBQQueryBuilder = new RuBQQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate);

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
    public void testWebServicePopulationOfFranceResultNumber() throws URISyntaxException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

        RuBQQueryBuilder ruBQQueryBuilder = new RuBQQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate);

        String question = "population of france";
        RuBQResult result0 = testWebService(ruBQQueryBuilder, question, langDefault);

        String expectetSparql = "PREFIX wdt: <http://www.wikidata.org/prop/direct/> PREFIX wd: <http://www.wikidata.org/entity/> SELECT ?uri WHERE { ?uri ?p ?o }";

        assertTrue(result0.getSparql().equals(expectetSparql));
    }

    private RuBQResult testWebService(RuBQQueryBuilder myApp, String question, String lang)
            throws URISyntaxException {
        RuBQResult result = myApp.requestRuBQWebService(this.endpoint, question, lang);
        assertFalse(result.getSparql().isEmpty());
        return result;
    }
}
