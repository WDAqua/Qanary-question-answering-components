package eu.wdaqua.qanary.rubq.wrapper;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.rubq.wrapper.messages.RuBQResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class MockRuBQQueryBuilderTest {
    private static final Logger logger = LoggerFactory.getLogger(MockRuBQQueryBuilderTest.class);
    private final String applicationName = "RuBQQueryBuilder";
    // name of query file
    private final String testQueryFilename = "/queries/test_query_of_rubq_api.rq";
    MockRestServiceServer mockServer;
    private URI endpoint;
    @Autowired
    private Environment env;
    private CacheOfRestTemplateResponse myCacheOfResponse = new CacheOfRestTemplateResponse();
    private RestTemplateWithCaching restTemplate = new RestTemplateWithCaching(this.myCacheOfResponse);

    @BeforeEach
    public void init() throws URISyntaxException, IOException {
        this.endpoint = new URI(env.getProperty("rubq.mock.endpoint.url"));
        logger.debug("endpoint: " + this.endpoint);

        assertNotNull(this.endpoint, "rubq.mock.endpoint.url cannot be empty");
        assertNotNull(this.restTemplate, "restTemplate cannot be null");

        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);

        this.mockServer.expect(requestTo(this.endpoint + "?question=Population%2520of%2520france?")).andExpect(method(org.springframework.http.HttpMethod.GET)).andRespond(withSuccess("{\"queries\":[\"" //
                        + RuBQTestConfiguration.getTestQuery(testQueryFilename) //
                        + "\"] }", //
                MediaType.APPLICATION_JSON));
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    void testWebServicePopulationOfFranceResultNumber() throws URISyntaxException, IOException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

        RuBQQueryBuilder ruBQQueryBuilder = new RuBQQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

        String question = "Population of france?";
        RuBQResult result = testWebService(ruBQQueryBuilder, question, langDefault);
        String expectedSparql = RuBQTestConfiguration.getTestQuery(testQueryFilename);

        assertEquals(expectedSparql, result.getSparql());
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
