package eu.wdaqua.qanary.component.dandelion.ned;

import com.google.gson.JsonObject;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.dandelion.ned.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.dandelion.ned.exception.ApiTokenIsNullOrEmptyException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class DandelionLiveTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandelionLiveTest.class);

    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${ned.dandelion.api.token}")
    private String apiToken;
    @Value("${ned.dandelion.api.live.test.active}")
    private boolean apiLiveTestActive;

    @Autowired
    private RestTemplateWithCaching myRestTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponses;
    @Autowired
    private Environment env;

    private DandelionNED dandelionNED;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init() throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.dandelionNED = new DandelionNED(
                applicationName, //
                myRestTemplate, //
                myCacheOfResponses, //
                false, //
                apiToken //
        );

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    @EnabledIf(
            expression = "#{environment['ned.dandelion.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {

        JsonObject response = this.dandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Albert Einstein"));

        ArrayList<DandelionNED.Link> links = this.dandelionNED.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (DandelionNED.Link l : links) {
            String sparql = this.dandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

//    @Test
//    @EnabledIf(
//            expression = "#{environment['ned.dandelion.api.live.test.active'] == 'true'}", //
//            loadContext = true
//    )
//    void testQuestion2()
//            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
//        JsonObject response = this.dandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question2"));
//        assertNotNull(response);
//        assertTrue(response.has("annotations"));
//        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());
//
//        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Germany"));
//
//        ArrayList<DandelionNED.Link> links = this.dandelionNED.getLinksFromAnnotation(response);
//        assertNotNull(links);
//        assertNotEquals(0, links.size());
//
//        for (DandelionNED.Link l : links) {
//            String sparql = this.dandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
//            assertNotNull(sparql);
//            assertNotEquals(0, sparql.length());
//        }
//    }

    @Test
    @EnabledIf(
            expression = "#{environment['ned.dandelion.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject response = this.dandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question3"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Batman"));

        ArrayList<DandelionNED.Link> links = this.dandelionNED.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (DandelionNED.Link l : links) {
            String sparql = this.dandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}