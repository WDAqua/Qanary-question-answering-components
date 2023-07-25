package eu.wdaqua.qanary.component.agdistis.ned;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.agdistis.ned.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.agdistis.ned.exception.ApiUrlIsNullOrEmptyException;
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
import java.util.List;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class AgdistisLiveTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgdistisLiveTest.class);

    private final String applicationName;
    private final boolean apiLiveTestActive;
    private final String apiUrl;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private Agdistis agdistis;
    private List<Agdistis.Spot> spots;
    private QanaryQuestion mockedQanaryQuestion;

    AgdistisLiveTest(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${agdistis.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${agdistis.api.url}") String apiUrl, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env
    ) {
        this.applicationName = applicationName;
        this.apiLiveTestActive = apiLiveTestActive;
        this.apiUrl = apiUrl;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws ApiLiveTestFaildException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, ApiUrlIsNullOrEmptyException {
        this.agdistis = new Agdistis(
                this.applicationName,
                this.myRestTemplate,
                this.myCacheOfResponses,
                false,
                this.apiUrl
        );

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));

        this.spots = new ArrayList<>();
    }

    @Test
    @EnabledIf(
            expression = "#{environment['agdistis.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {

        JsonArray response = this.agdistis.sendRequestToAPI(this.spots, this.env.getProperty("question1"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Albert Einstein"));

        List<Agdistis.Link> links = this.agdistis.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (Agdistis.Link l : links) {
            String sparql = this.agdistis.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['agdistis.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonArray response = this.agdistis.sendRequestToAPI(this.spots, this.env.getProperty("question2"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Germany"));

        List<Agdistis.Link> links = this.agdistis.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (Agdistis.Link l : links) {
            String sparql = this.agdistis.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @Disabled("unsupported question")
    @EnabledIf(
            expression = "#{environment['agdistis.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonArray response = this.agdistis.sendRequestToAPI(this.spots, this.env.getProperty("question3"));
        assertNotNull(response);
        assertNotEquals(0, response.size());

        assertTrue(response.toString().contains("Batman"));

        List<Agdistis.Link> links = this.agdistis.getLinksFromAnnotation(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());

        for (Agdistis.Link l : links) {
            String sparql = this.agdistis.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}