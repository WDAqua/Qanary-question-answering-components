package eu.wdaqua.qanary.g_answer_wrapper;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.g_answer_wrapper.messages.GAnswerResult;
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
public class TestMockGAnswerQueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TestMockGAnswerQueryBuilder.class);
    private final String applicationName = "PlatypusQueryBuilder";
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
        this.endpoint = new URI(env.getProperty("g_answer.endpoint.url"));
        assert (this.endpoint != null) : "g_answer.endpoint.url cannot be empty";

        assert this.restTemplate != null : "restTemplate cannot be null";

        mockServer = MockRestServiceServer.createServer(mockedRestTemplate);
        mockServer.expect(requestTo("http://some-ganswer-endpoint-url.com/endpoint?question=population%2520of%2520france"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"queries\":[\"SELECT ?uri WHERE { ?uri ?p ?o } \"]}", MediaType.APPLICATION_JSON));
    }

    /**
     * @throws URISyntaxException
     */
    @Test
    public void testWebServicePopulationOfFranceResultNumber() throws URISyntaxException {
        float threshold = 0.5f;
        String langDefault = "en";
        ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

        GAnswerQueryBuilder gAnswerQueryBuilder = new GAnswerQueryBuilder(threshold, langDefault, supportedLang, this.endpoint, this.applicationName, this.restTemplate, this.myCacheOfResponse);

        String question = "population of france";
        GAnswerResult result0 = testWebService(gAnswerQueryBuilder, question, langDefault);

        String expectetSparql = "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
                "PREFIX wds: <http://www.wikidata.org/entity/statement/>\n" +
                "PREFIX wdv: <http://www.wikidata.org/value/>\n" +
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
                "PREFIX p: <http://www.wikidata.org/prop/>\n" +
                "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n" +
                "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
                "PREFIX wdref: <http://www.wikidata.org/reference/>\n" +
                "PREFIX psv: <http://www.wikidata.org/prop/statement/value/>\n" +
                "PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>\n" +
                "PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>\n" +
                "PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>\n" +
                "PREFIX pr: <http://www.wikidata.org/prop/reference/>\n" +
                "PREFIX prv: <http://www.wikidata.org/prop/reference/value/>\n" +
                "PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>\n" +
                "PREFIX wdno: <http://www.wikidata.org/prop/novalue/>\n" +
                "PREFIX wdata: <http://www.wikidata.org/wiki/Special:EntityData/>\n" +
                "PREFIX schema: <http://schema.org/>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX prov: <http://www.w3.org/ns/prov#>\n" +
                "PREFIX bds: <http://www.bigdata.com/rdf/search#>\n" +
                "PREFIX gas: <http://www.bigdata.com/rdf/gas#>\n" +
                "PREFIX hint: <http://www.bigdata.com/queryHints#>\n" +
                "SELECT ?uri WHERE { ?uri ?p ?o } ";

        assertEquals(expectetSparql, result0.getSparql());
    }

    private GAnswerResult testWebService(GAnswerQueryBuilder myApp, String question, String lang)
            throws URISyntaxException {
        GAnswerResult result = myApp.requestGAnswerWebService(this.endpoint, question, lang);
        assertFalse(result.getSparql().isEmpty());
        return result;
    }

}
