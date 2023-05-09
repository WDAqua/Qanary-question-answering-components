package eu.wdaqua.qanary.component.babelfy.ned;

import com.google.gson.JsonArray;
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
class BabelfyLiveTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(BabelfyLiveTests.class);

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private Environment env;

    private BabelfyNED babelfyNED;
    private BabelfyServiceFetcher babelfyServiceFetcher;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init(
            @Autowired BabelfyServiceFetcher babelfyServiceFetcher //
    ) throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.babelfyNED = new BabelfyNED(
                applicationName, //
                babelfyServiceFetcher
        );

        this.babelfyServiceFetcher = babelfyServiceFetcher;

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Test
    @EnabledIf(
            expression = "#{environment['babelfy.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1() throws IOException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {

        JsonArray response = this.babelfyServiceFetcher.sendRequestToApi(this.env.getProperty("question1"));
        ArrayList<BabelfyServiceFetcher.Link> links = this.babelfyServiceFetcher.getLinksForQuestion(response);
        assertNotNull(links);
        assertNotEquals(0, links.size());
        assertTrue(this.checkLinkArrayListForContainsString(links, "Albert_Einstein"));

        for (BabelfyServiceFetcher.Link l : links) {
            String sparql = this.babelfyNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Test
    @EnabledIf(
            expression = "#{environment['babelfy.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2() throws IOException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        JsonArray response = this.babelfyServiceFetcher.sendRequestToApi(this.env.getProperty("question2"));
        ArrayList<BabelfyServiceFetcher.Link> links = this.babelfyServiceFetcher.getLinksForQuestion(response);

        assertNotNull(links);
        assertNotEquals(0, links.size());
        assertTrue(this.checkLinkArrayListForContainsString(links, "Germany"));

        for (BabelfyServiceFetcher.Link l : links) {
            String sparql = this.babelfyNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    @Disabled("Question is not supported by the API.")
    @Test
    @EnabledIf(
            expression = "#{environment['babelfy.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3() throws IOException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        JsonArray response = this.babelfyServiceFetcher.sendRequestToApi(this.env.getProperty("question3"));
        ArrayList<BabelfyServiceFetcher.Link> links = this.babelfyServiceFetcher.getLinksForQuestion(response);

        assertNotNull(links);
        assertNotEquals(0, links.size());
        assertTrue(this.checkLinkArrayListForContainsString(links, "Batman"));

        for (BabelfyServiceFetcher.Link l : links) {
            String sparql = this.babelfyNED.getSparqlInsertQuery(l, this.mockedQanaryQuestion);
            assertNotNull(sparql);
            assertNotEquals(0, sparql.length());
        }
    }

    private boolean checkLinkArrayListForContainsString(ArrayList<BabelfyServiceFetcher.Link> links, String containsToCheck) {
        for (BabelfyServiceFetcher.Link link : links) {
            if (link.link.contains(containsToCheck)) {
                return true;
            }
        }

        return false;
    }

}