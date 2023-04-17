package eu.wdaqua.qanary.component.dandelion.ner;

import com.google.gson.JsonObject;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.dandelion.ner.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.dandelion.ner.exception.ApiTokenIsNullOrEmptyException;
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

    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${ner.dandelion.api.token}")
    private String apiToken;

    @Autowired
    private RestTemplateWithCaching myRestTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponses;
    @Autowired
    private Environment env;

    private Dandelion dandelion;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException {
        this.dandelion = new Dandelion(
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
            expression = "#{environment['ner.dandelion.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {

        JsonObject response = this.dandelion.sendRequestToDandelionAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Albert Einstein"));

        ArrayList<Dandelion.Selection> selections = this.dandelion.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (Dandelion.Selection s : selections) {
            String sparql = this.dandelion.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Disabled("Question is not supportet.")
    @Test
    @EnabledIf(
            expression = "#{environment['ner.dandelion.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject response = this.dandelion.sendRequestToDandelionAPI(this.env.getProperty("question2"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Germany"));

        ArrayList<Dandelion.Selection> selections = this.dandelion.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (Dandelion.Selection s : selections) {
            String sparql = this.dandelion.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['ner.dandelion.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject response = this.dandelion.sendRequestToDandelionAPI(this.env.getProperty("question3"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Batman"));

        ArrayList<Dandelion.Selection> selections = this.dandelion.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (Dandelion.Selection s : selections) {
            String sparql = this.dandelion.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}