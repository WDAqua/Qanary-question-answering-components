package eu.wdaqua.qanary.rubq_wrapper;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.rubq_wrapper.messages.RuBQResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestMockRuBQQueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TestMockRuBQQueryBuilder.class);
    private final String applicationName = "RuBQQueryBuilder";
    MockRestServiceServer mockServer;
    private URI endpoint;
    @Autowired
    private Environment env;
    @Autowired
    private RestTemplateWithCaching restTemplate;
    @Autowired
    @Mock
    private RestTemplateWithCaching mockedRestTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponse;

    @Before
    public void init() throws URISyntaxException {
        this.endpoint = new URI(env.getProperty("rubq.endpoint.url"));
        assert (this.endpoint != null) : "rubq.endpoint.url cannot be empty";

        assert this.restTemplate != null : "restTemplate cannot be null";

        mockServer = MockRestServiceServer.createServer(mockedRestTemplate);
        mockServer.expect(requestTo("http://some-rubq-endpoint-url.com/endpoint?question=population%2520of%2520france"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"queries\":[\"PREFIX wdt: <http:\\/\\/www.wikidata.org\\/prop\\/direct\\/> PREFIX wd: <http:\\/\\/www.wikidata.org\\/entity\\/> SELECT ?uri WHERE { ?uri ?p ?o }\"]}", MediaType.APPLICATION_JSON));
    }

    /**
     * @throws URISyntaxException
     */
    @Test
    public void testWebServicePopulationOfFranceResultNumber() throws URISyntaxException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

        RuBQQueryBuilder ruBQQueryBuilder = new RuBQQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        String question = "population of france";
        RuBQResult result0 = testWebService(ruBQQueryBuilder, question, langDefault);

        String expectedSparql = "PREFIX wdt: <http://www.wikidata.org/prop/direct/> PREFIX wd: <http://www.wikidata.org/entity/> SELECT ?uri WHERE { ?uri ?p ?o }";

        assertEquals(result0.getSparql(), expectedSparql);
    }

    private RuBQResult testWebService(RuBQQueryBuilder myApp, String question, String lang) throws URISyntaxException {
        RuBQResult result = myApp.requestRuBQWebService(this.endpoint, question, lang);
        assertFalse(result.getSparql().isEmpty());
        return result;
    }

}
