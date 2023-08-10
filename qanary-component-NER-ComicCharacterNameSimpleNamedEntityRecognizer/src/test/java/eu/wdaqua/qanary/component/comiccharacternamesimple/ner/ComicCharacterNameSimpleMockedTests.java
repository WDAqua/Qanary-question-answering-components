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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class ComicCharacterNameSimpleMockedTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComicCharacterNameSimpleMockedTests.class);

    @Value("${spring.application.name}")
    private String applicationName;
    @Autowired
    private Environment env;

    private ComicCharacterNameSimpleNamedEntityRecognizer mockedComicCharacterNameSimpleNamedEntityRecognizer;
    private QanaryQuestion mockedQanaryQuestion;

    @BeforeEach
    public void init() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed {
        this.mockedComicCharacterNameSimpleNamedEntityRecognizer = Mockito.mock(ComicCharacterNameSimpleNamedEntityRecognizer.class);
        ReflectionTestUtils.setField(this.mockedComicCharacterNameSimpleNamedEntityRecognizer, "applicationName", this.applicationName);
        ReflectionTestUtils.setField(this.mockedComicCharacterNameSimpleNamedEntityRecognizer, "FILENAME_INSERT_ANNOTATION", "/queries/insert_one_annotation.rq");
        ReflectionTestUtils.setField(this.mockedComicCharacterNameSimpleNamedEntityRecognizer, "FILENAME_SELECT_HEROS", "/queries/select_all_superhero.rq");

        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
    }

    @Disabled("Question is not supported by the Component.")
    @Test
    void testQuestion1() throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        SuperheroNamedEntityFound mockedSuperheroNamedEntityFound = Mockito.mock(SuperheroNamedEntityFound.class);
        Mockito.when(mockedSuperheroNamedEntityFound.getSuperheroLabel()).thenReturn("Albert_Einstein");
        Mockito.when(mockedSuperheroNamedEntityFound.getResource()).thenReturn("http://dbpedia.org/resource/Albert_Einstein");
        Mockito.when(mockedSuperheroNamedEntityFound.getBeginIndex()).thenReturn(28);
        Mockito.when(mockedSuperheroNamedEntityFound.getEndIndex()).thenReturn(41);

        Mockito.when(this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(any(String.class))).thenReturn(mockedSuperheroNamedEntityFound);
        Mockito.when(this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(any(SuperheroNamedEntityFound.class), any(QanaryQuestion.class))).thenCallRealMethod();


        SuperheroNamedEntityFound response = this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(this.env.getProperty("question1"));
        assertNotNull(response);
        assertNotNull(response.getSuperheroLabel());
        assertNotNull(response.getResource());

        assertNotEquals(0, response.getBeginIndex());
        assertNotEquals(0, response.getEndIndex());

        assertTrue(response.getSuperheroLabel().contains("Albert_Einstein"));


        String sparql = this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(response, this.mockedQanaryQuestion);
        assertNotNull(sparql);
        assertNotEquals(0, sparql.length());
    }

    @Disabled("Question is not supported by the Component.")
    @Test
    void testQuestion2()
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException, SparqlQueryFailed {
        SuperheroNamedEntityFound mockedSuperheroNamedEntityFound = Mockito.mock(SuperheroNamedEntityFound.class);
        Mockito.when(mockedSuperheroNamedEntityFound.getSuperheroLabel()).thenReturn("Germany");
        Mockito.when(mockedSuperheroNamedEntityFound.getResource()).thenReturn("http://dbpedia.org/resource/Germany");
        Mockito.when(mockedSuperheroNamedEntityFound.getBeginIndex()).thenReturn(23);
        Mockito.when(mockedSuperheroNamedEntityFound.getEndIndex()).thenReturn(39);

        Mockito.when(this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(any(String.class))).thenReturn(mockedSuperheroNamedEntityFound);
        Mockito.when(this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(any(SuperheroNamedEntityFound.class), any(QanaryQuestion.class))).thenCallRealMethod();


        SuperheroNamedEntityFound response = this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(this.env.getProperty("question2"));
        assertNotNull(response);
        assertNotNull(response.getSuperheroLabel());
        assertNotNull(response.getResource());

        assertNotEquals(0, response.getBeginIndex());
        assertNotEquals(0, response.getEndIndex());

        assertTrue(response.getSuperheroLabel().contains("Germany"));


        String sparql = this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(response, this.mockedQanaryQuestion);
        assertNotNull(sparql);
        assertNotEquals(0, sparql.length());
    }

    @Test
    void testQuestion3() throws IOException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
        SuperheroNamedEntityFound mockedSuperheroNamedEntityFound = Mockito.mock(SuperheroNamedEntityFound.class);
        Mockito.when(mockedSuperheroNamedEntityFound.getSuperheroLabel()).thenReturn("Batman");
        Mockito.when(mockedSuperheroNamedEntityFound.getResource()).thenReturn("http://dbpedia.org/resource/Batman");
        Mockito.when(mockedSuperheroNamedEntityFound.getBeginIndex()).thenReturn(25);
        Mockito.when(mockedSuperheroNamedEntityFound.getEndIndex()).thenReturn(31);

        Mockito.when(this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(any(String.class))).thenReturn(mockedSuperheroNamedEntityFound);
        Mockito.when(this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(any(SuperheroNamedEntityFound.class), any(QanaryQuestion.class))).thenCallRealMethod();


        SuperheroNamedEntityFound response = this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getAllSuperheroNamesFromDBpediaMatchingPositions(this.env.getProperty("question3"));
        assertNotNull(response);
        assertNotNull(response.getSuperheroLabel());
        assertNotNull(response.getResource());

        assertNotEquals(0, response.getBeginIndex());
        assertNotEquals(0, response.getEndIndex());

        assertTrue(response.getSuperheroLabel().contains("Batman"));


        String sparql = this.mockedComicCharacterNameSimpleNamedEntityRecognizer.getSparqlInsertQuery(response, this.mockedQanaryQuestion);
        assertNotNull(sparql);
        assertNotEquals(0, sparql.length());
    }

}
