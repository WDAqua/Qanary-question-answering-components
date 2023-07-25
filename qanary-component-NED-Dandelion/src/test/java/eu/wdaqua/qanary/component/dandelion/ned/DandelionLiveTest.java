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
import org.junit.jupiter.api.Disabled;
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

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class DandelionLiveTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandelionLiveTest.class);

    private final String applicationName;
    private final String apiKey;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private DandelionNED dandelionNED;
    private QanaryQuestion mockedQanaryQuestion;

    DandelionLiveTest(
            @Value("${spring.application.name}") String applicationName, //
            @Value("${dandelion.api.key}") String apiKey, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env //
    ) {
        this.applicationName = applicationName;
        this.apiKey = apiKey;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.dandelionNED = new DandelionNED(
                applicationName, //
                myRestTemplate, //
                myCacheOfResponses, //
                false, //
                apiKey //
        );

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    @EnabledIf(
            expression = "#{environment['dandelion.api.live.test.active'] == 'true'}", //
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

    @Test
    @Disabled("Unsupported question")
    @EnabledIf(
            expression = "#{environment['dandelion.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject response = this.dandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question2"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Germany"));

        ArrayList<DandelionNED.Link> links = this.dandelionNED.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (DandelionNED.Link l : links) {
            String sparql = this.dandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['dandelion.api.live.test.active'] == 'true'}", //
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