package eu.wdaqua.qanary.component.dandelion.ned;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class DandelionMockedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandelionMockedTest.class);

    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${ned.dandelion.api.token}")
    private String apiToken;

    @Autowired
    private RestTemplateWithCaching myRestTemplate;
    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponses;
    @Autowired
    private Environment env;

    private DandelionNED mockedDandelionNED;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.mockedDandelionNED = Mockito.mock(DandelionNED.class);
        ReflectionTestUtils.setField(this.mockedDandelionNED, "applicationName", this.applicationName);
        ReflectionTestUtils.setField(this.mockedDandelionNED, "myRestTemplate", myRestTemplate);
        ReflectionTestUtils.setField(this.mockedDandelionNED, "myCacheOfResponses", myCacheOfResponses);
        ReflectionTestUtils.setField(this.mockedDandelionNED, "apiToken", apiToken);
        ReflectionTestUtils.setField(this.mockedDandelionNED, "FILENAME_INSERT_ANNOTATION", "/queries/insert_one_annotation.rq");

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject mockedResponseJsonObject = JsonParser.parseString(DandelionTestConfiguration.getStringFromFile("response/api_response_qestion_1.json")).getAsJsonObject();

        Mockito.when(this.mockedDandelionNED.sendRequestToDandelionAPI(any(String.class))).thenReturn(mockedResponseJsonObject);
        Mockito.when(this.mockedDandelionNED.getLinksFromAnnotation(any(JsonObject.class))).thenCallRealMethod();
        Mockito.when(this.mockedDandelionNED.getSparqlInsertQuery(any(DandelionNED.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonObject response = this.mockedDandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Albert Einstein"));

        ArrayList<DandelionNED.Link> links = this.mockedDandelionNED.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (DandelionNED.Link l : links) {
            String sparql = this.mockedDandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

//    @Test
//    void testQuestion2()
//            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
//        JsonObject mockedResponseJsonObject = JsonParser.parseString(DandelionTestConfiguration.getStringFromFile("response/api_response_qestion_2.json")).getAsJsonObject();
//
//        Mockito.when(this.mockedDandelionNED.sendRequestToDandelionAPI(any(String.class))).thenReturn(mockedResponseJsonObject);
//        Mockito.when(this.mockedDandelionNED.getLinksFromAnnotation(any(JsonObject.class))).thenCallRealMethod();
//        Mockito.when(this.mockedDandelionNED.getSparqlInsertQuery(any(DandelionNED.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();
//
//
//        JsonObject response = this.mockedDandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question2"));
//        assertNotNull(response);
//        assertTrue(response.has("annotations"));
//        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());
//
//        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Germany"));
//
//        ArrayList<DandelionNED.Link> links = this.mockedDandelionNED.getLinksFromAnnotation(response);
//        assertNotNull(links);
//        assertNotEquals(0, links.size());
//
//        for (DandelionNED.Link l : links) {
//            String sparql = this.mockedDandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
//            assertNotNull(sparql);
//            assertNotEquals(0, sparql.length());
//        }
//    }

    @Test
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject mockedResponseJsonObject = JsonParser.parseString(DandelionTestConfiguration.getStringFromFile("response/api_response_qestion_3.json")).getAsJsonObject();

        Mockito.when(this.mockedDandelionNED.sendRequestToDandelionAPI(any(String.class))).thenReturn(mockedResponseJsonObject);
        Mockito.when(this.mockedDandelionNED.getLinksFromAnnotation(any(JsonObject.class))).thenCallRealMethod();
        Mockito.when(this.mockedDandelionNED.getSparqlInsertQuery(any(DandelionNED.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonObject response = this.mockedDandelionNED.sendRequestToDandelionAPI(this.env.getProperty("question3"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.get("annotations").getAsJsonArray().size());

        assertTrue(response.get("annotations").getAsJsonArray().toString().contains("Batman"));

        ArrayList<DandelionNED.Link> links = this.mockedDandelionNED.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (DandelionNED.Link l : links) {
            String sparql = this.mockedDandelionNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}
