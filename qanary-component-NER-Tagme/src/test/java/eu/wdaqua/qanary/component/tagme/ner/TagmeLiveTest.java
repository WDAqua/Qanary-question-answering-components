package eu.wdaqua.qanary.component.tagme.ner;

import com.google.gson.JsonObject;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.tagme.ner.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.tagme.ner.exception.ApiTokenIsNullOrEmptyException;
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
import java.util.List;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class TagmeLiveTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagmeLiveTest.class);

    private final String applicationName;
    private final boolean apiLiveTestActive;
    private final String apiUrl;
    private final String apiKey;
    private final String threshold;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private TagmeNER tagme;
    private QanaryQuestion mockedQanaryQuestion;

    TagmeLiveTest(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${tagme.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${tagme.api.url}") String apiUrl, //
            @Value("${tagme.api.key}") String apiKey, //
            @Value("${tagme.api.threshold}") final String threshold, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env
    ) {
        this.applicationName = applicationName;
        this.apiLiveTestActive = apiLiveTestActive;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.threshold = threshold;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
        this.tagme = new TagmeNER(
                this.applicationName,
                false,
                this.apiUrl,
                this.apiKey,
                this.threshold,
                this.myRestTemplate,
                this.myCacheOfResponses
        );

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    @EnabledIf(
            expression = "#{environment['tagme.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {

        JsonObject response = this.tagme.sendRequestToAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.getAsJsonArray("annotations").size());

        assertTrue(response.getAsJsonArray("annotations").toString().contains("Albert Einstein"));

        List<TagmeNER.Selection> selections = this.tagme.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TagmeNER.Selection s : selections) {
            String sparql = this.tagme.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['tagme.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject response = this.tagme.sendRequestToAPI(this.env.getProperty("question2"));
        assertNotNull(response);
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.getAsJsonArray("annotations").size());

        assertTrue(response.getAsJsonArray("annotations").toString().contains("Germany"));

        List<TagmeNER.Selection> selections = this.tagme.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TagmeNER.Selection s : selections) {
            String sparql = this.tagme.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @Disabled("Unsupported question")
    @EnabledIf(
            expression = "#{environment['tagme.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject response = this.tagme.sendRequestToAPI(this.env.getProperty("question3"));
        assertTrue(response.has("annotations"));
        assertNotEquals(0, response.getAsJsonArray("annotations").size());

        assertTrue(response.getAsJsonArray("annotations").toString().contains("Batman"));

        List<TagmeNER.Selection> selections = this.tagme.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TagmeNER.Selection l : selections) {
            String sparql = this.tagme.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}