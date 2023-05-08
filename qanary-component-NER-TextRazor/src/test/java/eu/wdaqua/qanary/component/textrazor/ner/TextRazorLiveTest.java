package eu.wdaqua.qanary.component.textrazor.ner;

import com.google.gson.JsonObject;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.textrazor.ner.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.textrazor.ner.exception.ApiTokenIsNullOrEmptyException;
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
class TextRazorLiveTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextRazorLiveTest.class);

    private final String applicationName;
    private final boolean apiLiveTestActive;
    private final String apiUrl;
    private final String apiKey;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private TextRazor textRazor;
    private QanaryQuestion mockedQanaryQuestion;

    TextRazorLiveTest(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${textrazor.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${textrazor.api.url}") String apiUrl, //
            @Value("${textrazor.api.key}") String apiKey, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env
    ) {
        this.applicationName = applicationName;
        this.apiLiveTestActive = apiLiveTestActive;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
        this.textRazor = new TextRazor(
                applicationName,
                false,
                apiUrl,
                apiKey,
                myRestTemplate,
                myCacheOfResponses
        );

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    @EnabledIf(
            expression = "#{environment['textrazor.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {

        JsonObject response = this.textRazor.sendRequestToAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("response"));
        assertTrue(response.getAsJsonObject("response").has("entities"));
        assertNotEquals(0, response.getAsJsonObject("response").get("entities").getAsJsonArray().size());

        assertTrue(response.getAsJsonObject("response").get("entities").getAsJsonArray().toString().contains("Albert Einstein"));

        List<TextRazor.Selection> selections = this.textRazor.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TextRazor.Selection s : selections) {
            String sparql = this.textRazor.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['textrazor.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject response = this.textRazor.sendRequestToAPI(this.env.getProperty("question2"));
        assertNotNull(response);
        assertTrue(response.has("response"));
        assertTrue(response.getAsJsonObject("response").has("entities"));
        assertNotEquals(0, response.getAsJsonObject("response").get("entities").getAsJsonArray().size());

        assertTrue(response.getAsJsonObject("response").get("entities").getAsJsonArray().toString().contains("Germany"));

        List<TextRazor.Selection> selections = this.textRazor.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TextRazor.Selection s : selections) {
            String sparql = this.textRazor.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @Disabled("Unsupported question")
    @EnabledIf(
            expression = "#{environment['textrazor.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject response = this.textRazor.sendRequestToAPI(this.env.getProperty("question3"));
        assertNotNull(response);
        assertTrue(response.has("entity_list"));
        assertNotEquals(0, response.get("entity_list").getAsJsonArray().size());

        assertTrue(response.get("entity_list").getAsJsonArray().toString().contains("Batman"));

        List<TextRazor.Selection> selections = this.textRazor.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (TextRazor.Selection l : selections) {
            String sparql = this.textRazor.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}