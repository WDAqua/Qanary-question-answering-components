package eu.wdaqua.qanary.component.tagme.ner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
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
import java.util.List;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class TagmeMockedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagmeMockedTest.class);

    private final String applicationName;
    private final boolean apiLiveTestActive;
    private final String apiKey;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private TagmeNER mockedTagme;
    private QanaryQuestion mockedQanaryQuestion;

    TagmeMockedTest(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${tagme.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${tagme.api.key}") String apiKey, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env
    ) {
        this.applicationName = applicationName;
        this.apiLiveTestActive = apiLiveTestActive;
        this.apiKey = apiKey;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.mockedTagme = Mockito.mock(TagmeNER.class);
        ReflectionTestUtils.setField(this.mockedTagme, "applicationName", this.applicationName);
        ReflectionTestUtils.setField(this.mockedTagme, "myRestTemplate", this.myRestTemplate);
        ReflectionTestUtils.setField(this.mockedTagme, "myCacheOfResponses", this.myCacheOfResponses);
        ReflectionTestUtils.setField(this.mockedTagme, "apiKey", this.apiKey);
        ReflectionTestUtils.setField(this.mockedTagme, "FILENAME_INSERT_ANNOTATION", "/queries/insert_one_annotation.rq");

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject mockedResponseJsonObject = JsonParser.parseString(TagmeTestConfiguration.getStringFromFile("response/api_response_qestion_1.json")).getAsJsonObject();

        Mockito.when(this.mockedTagme.sendRequestToAPI(any(String.class))).thenReturn(mockedResponseJsonObject);
        Mockito.when(this.mockedTagme.getSelectionsFromAnnotation(any(JsonObject.class))).thenCallRealMethod();
        Mockito.when(this.mockedTagme.getSparqlInsertQuery(any(TagmeNER.Selection.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonObject response = this.mockedTagme.sendRequestToAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.getAsJsonArray("annotations").size());

        assertTrue(response.getAsJsonArray("annotations").toString().contains("Albert Einstein"));

        List<TagmeNER.Selection> selections = this.mockedTagme.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TagmeNER.Selection s : selections) {
            String sparql = this.mockedTagme.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject mockedResponseJsonObject = JsonParser.parseString(TagmeTestConfiguration.getStringFromFile("response/api_response_qestion_2.json")).getAsJsonObject();

        Mockito.when(this.mockedTagme.sendRequestToAPI(any(String.class))).thenReturn(mockedResponseJsonObject);
        Mockito.when(this.mockedTagme.getSelectionsFromAnnotation(any(JsonObject.class))).thenCallRealMethod();
        Mockito.when(this.mockedTagme.getSparqlInsertQuery(any(TagmeNER.Selection.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonObject response = this.mockedTagme.sendRequestToAPI(this.env.getProperty("question2"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.getAsJsonArray("annotations").size());

        assertTrue(response.getAsJsonArray("annotations").toString().contains("Germany"));

        List<TagmeNER.Selection> selections = this.mockedTagme.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TagmeNER.Selection s : selections) {
            String sparql = this.mockedTagme.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @Disabled("Unsupported question")
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject mockedResponseJsonObject = JsonParser.parseString(TagmeTestConfiguration.getStringFromFile("response/api_response_qestion_3.json")).getAsJsonObject();

        Mockito.when(this.mockedTagme.sendRequestToAPI(any(String.class))).thenReturn(mockedResponseJsonObject);
        Mockito.when(this.mockedTagme.getSelectionsFromAnnotation(any(JsonObject.class))).thenCallRealMethod();
        Mockito.when(this.mockedTagme.getSparqlInsertQuery(any(TagmeNER.Selection.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonObject response = this.mockedTagme.sendRequestToAPI(this.env.getProperty("question3"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.getAsJsonArray("annotations").size());

        assertTrue(response.getAsJsonArray("annotations").toString().contains("Batman"));

        List<TagmeNER.Selection> selections = this.mockedTagme.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TagmeNER.Selection s : selections) {
            String sparql = this.mockedTagme.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}
