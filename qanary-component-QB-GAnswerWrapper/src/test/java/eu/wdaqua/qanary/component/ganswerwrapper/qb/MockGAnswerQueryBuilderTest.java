package eu.wdaqua.qanary.component.ganswerwrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.ganswerwrapper.qb.messages.GAnswerResult;
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
class MockGAnswerQueryBuilderTest {
    private static final Logger logger = LoggerFactory.getLogger(MockGAnswerQueryBuilderTest.class);
    private final String applicationName = "GAnswerQueryBuilder";
    // name of query file
    private final String testQueryFilename = "/queries/test_query_of_gAnswer_api.rq";
    MockRestServiceServer mockServer;
    private URI endpoint;
    @Autowired
    private Environment env;
    private CacheOfRestTemplateResponse myCacheOfResponse = new CacheOfRestTemplateResponse();
    private RestTemplateWithCaching restTemplate = new RestTemplateWithCaching(this.myCacheOfResponse, "caching");

    @BeforeEach
    public void init() throws URISyntaxException, IOException {
        this.endpoint = new URI(env.getProperty("g_answer.mock.endpoint.url"));
        logger.debug("endpoint: " + this.endpoint);

        assertNotNull(this.endpoint, "g_answer.mock.endpoint.url cannot be empty");
        assertNotNull(this.restTemplate, "restTemplate cannot be null");

        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);

        this.mockServer.expect(requestTo(this.endpoint + "?question=Population%2520of%2520france?")).andExpect(method(org.springframework.http.HttpMethod.GET)).andRespond(withSuccess("{\"queries\":[\"" //
                        + GAnswerTestConfiguration.getTestQuery(testQueryFilename) //
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

        GAnswerQueryBuilder gAnswerQueryBuilder = new GAnswerQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, this.myCacheOfResponse);

        String question = "Population of france?";
        GAnswerResult result = testWebService(gAnswerQueryBuilder, question, langDefault);
        String expectedSparql = GAnswerTestConfiguration.getTestQuery(testQueryFilename);

        assertEquals(expectedSparql, result.getSparql());
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
