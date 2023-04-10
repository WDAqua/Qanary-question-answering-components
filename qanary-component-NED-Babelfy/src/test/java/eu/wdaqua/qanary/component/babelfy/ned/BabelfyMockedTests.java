package eu.wdaqua.qanary.component.babelfy.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class BabelfyMockedTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(BabelfyMockedTests.class);

    @Value("${spring.application.name}")
    private String applicationName;
    @Autowired
    private Environment env;

    private BabelfyNED mockedBabelfyNED;
    private BabelfyServiceFetcher mockedBabelfyServiceFetcher;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.mockedBabelfyServiceFetcher = Mockito.mock(BabelfyServiceFetcher.class);

        this.mockedBabelfyNED = Mockito.mock(BabelfyNED.class);
        ReflectionTestUtils.setField(this.mockedBabelfyNED, "applicationName", this.applicationName);
        ReflectionTestUtils.setField(this.mockedBabelfyNED, "babelfyServiceFetcher", this.mockedBabelfyServiceFetcher);
        ReflectionTestUtils.setField(this.mockedBabelfyNED, "FILENAME_INSERT_ANNOTATION", "/queries/insert_one_annotation.rq");

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonArray mockedResponseJsonArray = JsonParser.parseString(BabelfyTestConfiguration.getStringFromFile("response/api_response_qestion_1.json")).getAsJsonArray();

        Mockito.when(this.mockedBabelfyServiceFetcher.sendRequestToApi(any(String.class))).thenReturn(mockedResponseJsonArray);
        Mockito.when(this.mockedBabelfyServiceFetcher.getLinksForQuestion(any(JsonArray.class))).thenCallRealMethod();
        Mockito.when(this.mockedBabelfyNED.getSparqlInsertQuery(any(BabelfyServiceFetcher.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonArray response = this.mockedBabelfyServiceFetcher.sendRequestToApi(this.env.getProperty("question1"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Albert_Einstein"));

        ArrayList<BabelfyServiceFetcher.Link> links = this.mockedBabelfyServiceFetcher.getLinksForQuestion(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (BabelfyServiceFetcher.Link l : links) {
            String sparql = this.mockedBabelfyNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonArray mockedResponseJsonArray = JsonParser.parseString(BabelfyTestConfiguration.getStringFromFile("response/api_response_qestion_2.json")).getAsJsonArray();

        Mockito.when(this.mockedBabelfyServiceFetcher.sendRequestToApi(any(String.class))).thenReturn(mockedResponseJsonArray);
        Mockito.when(this.mockedBabelfyServiceFetcher.getLinksForQuestion(any(JsonArray.class))).thenCallRealMethod();
        Mockito.when(this.mockedBabelfyNED.getSparqlInsertQuery(any(BabelfyServiceFetcher.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonArray response = this.mockedBabelfyServiceFetcher.sendRequestToApi(this.env.getProperty("question2"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Germany"));

        ArrayList<BabelfyServiceFetcher.Link> links = this.mockedBabelfyServiceFetcher.getLinksForQuestion(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (BabelfyServiceFetcher.Link l : links) {
            String sparql = this.mockedBabelfyNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Disabled("Question is not supported by the API.")
    @Test
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonArray mockedResponseJsonArray = JsonParser.parseString(BabelfyTestConfiguration.getStringFromFile("response/api_response_qestion_3.json")).getAsJsonArray();

        Mockito.when(this.mockedBabelfyServiceFetcher.sendRequestToApi(any(String.class))).thenReturn(mockedResponseJsonArray);
        Mockito.when(this.mockedBabelfyServiceFetcher.getLinksForQuestion(any(JsonArray.class))).thenCallRealMethod();
        Mockito.when(this.mockedBabelfyNED.getSparqlInsertQuery(any(BabelfyServiceFetcher.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonArray response = this.mockedBabelfyServiceFetcher.sendRequestToApi(this.env.getProperty("question3"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Batman"));

        ArrayList<BabelfyServiceFetcher.Link> links = this.mockedBabelfyServiceFetcher.getLinksForQuestion(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (BabelfyServiceFetcher.Link l : links) {
            String sparql = this.mockedBabelfyNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}
