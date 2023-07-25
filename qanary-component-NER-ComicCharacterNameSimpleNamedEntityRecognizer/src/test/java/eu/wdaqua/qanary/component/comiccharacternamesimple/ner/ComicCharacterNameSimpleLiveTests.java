package eu.wdaqua.qanary.component.comiccharacternamesimple.ner;

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

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class ComicCharacterNameSimpleLiveTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComicCharacterNameSimpleLiveTests.class);

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private Environment env;

    private ComicCharacterNameSimpleNamedEntityRecognizer comicCharacterNameSimpleNamedEntityRecognizer;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init(
            @Autowired ComicCharacterNameSimpleNamedEntityRecognizer comicCharacterNameSimpleNamedEntityRecognizer //
    ) throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {

        this.comicCharacterNameSimpleNamedEntityRecognizer = comicCharacterNameSimpleNamedEntityRecognizer;

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Disabled("Question is not supported by the Component.")
    @Test
    @EnabledIf(
            expression = "#{environment['ner.comicCharacterNameSimpleNamedEntityRecognizer.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion1() throws IOException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {

        SuperheroNamedEntityFound response = this.comicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(this.env.getProperty("question1"));

        assertNotNull(response);
        assertNotNull(response.getSuperheroLabel());
        assertNotNull(response.getResource());

        assertNotEquals(0, response.getBeginIndex());
        assertNotEquals(0, response.getEndIndex());

        assertTrue(response.getSuperheroLabel().contains("Albert Einstein"));


        String sparql = this.comicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(response, this.mockedQanaryQuestion);
        assertNotNull(sparql);
        assertNotEquals(0, sparql.length());
    }

    @Disabled("Question is not supported by the Component.")
    @Test
    @EnabledIf(
            expression = "#{environment['ner.comicCharacterNameSimpleNamedEntityRecognizer.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion2() throws IOException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        SuperheroNamedEntityFound response = this.comicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(this.env.getProperty("question2"));

        assertNotNull(response);
        assertNotNull(response.getSuperheroLabel());
        assertNotNull(response.getResource());

        assertNotEquals(0, response.getBeginIndex());
        assertNotEquals(0, response.getEndIndex());

        assertTrue(response.getSuperheroLabel().contains("Germany"));


        String sparql = this.comicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(response, this.mockedQanaryQuestion);
        assertNotNull(sparql);
        assertNotEquals(0, sparql.length());
    }

    @Test
    @EnabledIf(
            expression = "#{environment['ner.comicCharacterNameSimpleNamedEntityRecognizer.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testQuestion3() throws IOException, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        SuperheroNamedEntityFound response = this.comicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(this.env.getProperty("question3"));

        assertNotNull(response);
        assertNotNull(response.getSuperheroLabel());
        assertNotNull(response.getResource());

        assertNotEquals(0, response.getBeginIndex());
        assertNotEquals(0, response.getEndIndex());

        assertTrue(response.getSuperheroLabel().contains("Batman"));


        String sparql = this.comicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(response, this.mockedQanaryQuestion);
        assertNotNull(sparql);
        assertNotEquals(0, sparql.length());
    }

}