package eu.wdaqua.qanary.component.meaningcloud.ner;

import com.google.gson.JsonObject;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.meaningcloud.ner.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.meaningcloud.ner.exception.ApiTokenIsNullOrEmptyException;
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
class MeaningCloudLiveTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeaningCloudLiveTest.class);

    private final String applicationName;
    private final boolean apiLiveTestActive;
    private final String meaningCloudKey;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private final Environment env;

    private MeaningCloud meaningCloud;
    private QanaryQuestion mockedQanaryQuestion;

    MeaningCloudLiveTest(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${meaningcloud.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${meaningcloud.api.key}") String meaningCloudKey, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Autowired Environment env
    ) {
        this.applicationName = applicationName;
        this.apiLiveTestActive = apiLiveTestActive;
        this.meaningCloudKey = meaningCloudKey;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.env = env;
    }

    @BeforeEach
    public void init() throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
        this.meaningCloud = new MeaningCloud(
                applicationName,
                false,
                meaningCloudKey,
                myRestTemplate,
                myCacheOfResponses
        );

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    @EnabledIf(
            expression = "#{environment['meaningcloud.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {

        JsonObject response = this.meaningCloud.sendRequestToAPI(this.env.getProperty("question1"));
        assertNotNull(response);
        assertTrue(response.has("entity_list"));
        assertNotEquals(0, response.get("entity_list").getAsJsonArray().size());

        assertTrue(response.get("entity_list").getAsJsonArray().toString().contains("Albert Einstein"));

        List<MeaningCloud.Selection> selections = this.meaningCloud.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (MeaningCloud.Selection s : selections) {
            String sparql = this.meaningCloud.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['meaningcloud.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        JsonObject response = this.meaningCloud.sendRequestToAPI(this.env.getProperty("question2"));
        assertNotNull(response);
        assertTrue(response.has("entity_list"));
        assertNotEquals(0, response.get("entity_list").getAsJsonArray().size());

        assertTrue(response.get("entity_list").getAsJsonArray().toString().contains("Germany"));

        List<MeaningCloud.Selection> selections = this.meaningCloud.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (MeaningCloud.Selection s : selections) {
            String sparql = this.meaningCloud.getSparqlInsertQuery(s, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @Disabled("Question is unsupported")
    @EnabledIf(
            expression = "#{environment['meaningcloud.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        JsonObject response = this.meaningCloud.sendRequestToAPI(this.env.getProperty("question3"));
        assertNotNull(response);
        assertTrue(response.has("entity_list"));
        assertNotEquals(0, response.get("entity_list").getAsJsonArray().size());

        assertTrue(response.get("entity_list").getAsJsonArray().toString().contains("Batman"));

        List<MeaningCloud.Selection> selections = this.meaningCloud.getSelectionsFromAnnotation(response);
        assertNotNull(selections);
        assertNotEquals(0, selections.size());

        for (MeaningCloud.Selection l : selections) {
            String sparql = this.meaningCloud.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

}