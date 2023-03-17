package eu.wdaqua.qanary.component.simplerealnameofsuperhero.qb;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class QueryBuilderTest {

    private static final Logger logger = LoggerFactory.getLogger(QueryBuilderTest.class);

    private QueryBuilderSimpleRealNameOfSuperHero qb;
    private String testQuery;

    @Before
    public void setUp() {
        qb = new QueryBuilderSimpleRealNameOfSuperHero("test.name");
        testQuery = "" +
                "PREFIX dbr: <http://dbpedia.org/resource/>\n" +
                "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT * WHERE {  \n" +
                "    ?resource foaf:name ?answer .  \n" +
                "    ?resource rdfs:label ?label .  \n" +
                "    FILTER(LANG(?label) = \"en\") .  \n" +
                "    ?resource dct:subject dbr:Category:Superheroes_with_alter_egos .  \n" +
                "    FILTER(! strStarts(LCASE(?label), LCASE(?answer))).  \n" +
                "    VALUES ?resource { <https://dbpedia.org/resource/Example> } .\n" +
                "} ORDER BY ?resource";

    }

    @Test
    public void testIsQuestionSupported() {
        String supportedQuestion1 = "What is the real name of Batman?";
        String supportedQuestion2 = "What is the real name of The Rock?";
        String unsupportedQuestion1 = "give me the real name of Batman";
        String unsupportedQuestion2 = "Where was Aquaman born?";

        assertTrue(qb.isQuestionSupported(supportedQuestion1));
        assertTrue(qb.isQuestionSupported(supportedQuestion2));

        assertFalse(qb.isQuestionSupported(unsupportedQuestion1));
        assertFalse(qb.isQuestionSupported(unsupportedQuestion2));
    }

    @Test
    public void testCreateAndInsertAnswerSPARQL() throws Exception {
        // given a QanaryMessage, QanaryQuestion and created Query
        QanaryMessage qanaryMessage = mock(QanaryMessage.class);
        QanaryQuestion<String> qanaryQuestion = mock(QanaryQuestion.class);

        URI ingraph = new URI("urn:qanary:ingraph");
        URI outgraph = new URI("urn:qanary:outgraph");
        URI uri = new URI("test_question_uri");

        when(qanaryMessage.getInGraph()).thenReturn(ingraph);
        when(qanaryMessage.getOutGraph()).thenReturn(outgraph);

        when(qanaryQuestion.getUri()).thenReturn(uri);
        when(qanaryQuestion.getInGraph()).thenReturn(ingraph);
        when(qanaryQuestion.getOutGraph()).thenReturn(outgraph);

        // when the insert query is created
        String sparqlInsert = qb.getInsertQuery(qanaryMessage, qanaryQuestion, testQuery);

        logger.info(testQuery);

        // then it contains the created dbpediaQuery, the question graph and question URI
        assertTrue(sparqlInsert.contains("GRAPH <" + outgraph.toString() + ">"));
        assertTrue(sparqlInsert.contains("oa:hasTarget <" + uri.toString() + ">"));
        assertTrue(sparqlInsert.contains("oa:hasBody \"" + testQuery.replace("\n", "\\n").replace("\"", "\\\"") + "\""));
    }
}
