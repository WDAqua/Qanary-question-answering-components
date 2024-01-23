package eu.wdaqua.qanary.component.agdistis.ned;

import com.google.gson.JsonArray;
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
import java.util.ArrayList;
import java.util.List;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class AgdistisMockedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgdistisMockedTest.class);

    private final String applicationName;
    private final boolean apiLiveTestActive;
    private final String apiUrl;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private Agdistis mockedAgdistis;
    private List<Agdistis.Spot> spots;
    private QanaryQuestion mockedQanaryQuestion;

    AgdistisMockedTest(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${agdistis.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${agdistis.api.url}") String url, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env
    ) {
        this.applicationName = applicationName;
        this.apiLiveTestActive = apiLiveTestActive;
        this.apiUrl = url;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.mockedAgdistis = Mockito.mock(Agdistis.class);
        ReflectionTestUtils.setField(this.mockedAgdistis, "applicationName", this.applicationName);
        ReflectionTestUtils.setField(this.mockedAgdistis, "myRestTemplate", this.myRestTemplate);
        ReflectionTestUtils.setField(this.mockedAgdistis, "myCacheOfResponses", this.myCacheOfResponses);
        ReflectionTestUtils.setField(this.mockedAgdistis, "apiUrl", this.apiUrl);
        ReflectionTestUtils.setField(this.mockedAgdistis, "FILENAME_SPOTS_FROM_GRAPH", "/queries/select_all_AnnotationOfSpotInstance.rq");
        ReflectionTestUtils.setField(this.mockedAgdistis, "FILENAME_INSERT_ANNOTATION", "/queries/insert_one_annotation.rq");

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));

        this.spots = new ArrayList<>();
    }

    @Test
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonArray mockedResponseJsonArray = JsonParser.parseString(AgdistisTestConfiguration.getStringFromFile("response/api_response_qestion_1.json")).getAsJsonArray();

        Mockito.when(this.mockedAgdistis.sendRequestToAPI(any(List.class), any(String.class))).thenReturn(mockedResponseJsonArray);
        Mockito.when(this.mockedAgdistis.getLinksFromAnnotation(any(JsonArray.class))).thenCallRealMethod();
        Mockito.when(this.mockedAgdistis.getSparqlInsertQuery(any(Agdistis.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonArray response = this.mockedAgdistis.sendRequestToAPI(this.spots, this.env.getProperty("question1"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Albert Einstein"));

        List<Agdistis.Link> links = this.mockedAgdistis.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (Agdistis.Link l : links) {
            String sparql = this.mockedAgdistis.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonArray mockedResponseJsonArray = JsonParser.parseString(AgdistisTestConfiguration.getStringFromFile("response/api_response_qestion_2.json")).getAsJsonArray();

        Mockito.when(this.mockedAgdistis.sendRequestToAPI(any(List.class), any(String.class))).thenReturn(mockedResponseJsonArray);
        Mockito.when(this.mockedAgdistis.getLinksFromAnnotation(any(JsonArray.class))).thenCallRealMethod();
        Mockito.when(this.mockedAgdistis.getSparqlInsertQuery(any(Agdistis.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonArray response = this.mockedAgdistis.sendRequestToAPI(this.spots, this.env.getProperty("question2"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Germany"));

        List<Agdistis.Link> links = this.mockedAgdistis.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (Agdistis.Link l : links) {
            String sparql = this.mockedAgdistis.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @Disabled("unsupported question")
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonArray mockedResponseJsonArray = JsonParser.parseString(AgdistisTestConfiguration.getStringFromFile("response/api_response_qestion_3.json")).getAsJsonArray();

        Mockito.when(this.mockedAgdistis.sendRequestToAPI(any(List.class), any(String.class))).thenReturn(mockedResponseJsonArray);
        Mockito.when(this.mockedAgdistis.getLinksFromAnnotation(any(JsonArray.class))).thenCallRealMethod();
        Mockito.when(this.mockedAgdistis.getSparqlInsertQuery(any(Agdistis.Link.class), any(QanaryQuestion.class))).thenCallRealMethod();


        JsonArray response = this.mockedAgdistis.sendRequestToAPI(this.spots, this.env.getProperty("question3"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Batman"));

        List<Agdistis.Link> links = this.mockedAgdistis.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (Agdistis.Link l : links) {
            String sparql = this.mockedAgdistis.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}
