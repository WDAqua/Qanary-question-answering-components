package eu.wdaqua.qanary.component.querybuilder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.Before;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;

import java.net.URI;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


public class TestQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(TestQueryBuilder.class);

    private QueryBuilderSimpleRealNameOfSuperHero qb;
    private String testQuery;

    @Before
    public void setUp() {
        qb = new QueryBuilderSimpleRealNameOfSuperHero("qb-simplerealname-test");
        testQuery = "" // 
					+ "PREFIX dbr: <http://dbpedia.org/resource/> \n" //
					+ "PREFIX dct: <http://purl.org/dc/terms/> \n" //
					+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" //
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" //
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" //
					+ "SELECT * WHERE {\n" //
					+ "  ?resource foaf:name ?answer .\n" // real name of superhero
					+ "  ?resource rdfs:label ?label .\n" // get the character name of the superhero
					+ "  FILTER(LANG(?label) = \"en\") .\n" // only English names
					+ "  ?resource dct:subject dbr:Category:Superheroes_with_alter_egos .\n" // only superheros
					+ "  FILTER(! strStarts(LCASE(?label), LCASE(?answer))).\n" // filter starting with the same name
					+ "  VALUES ?resource { <https://dbpedia.org/resource/Example> } .\n" // only for this specific resource
					+ "} \n" //
					+ "ORDER BY ?resource";
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
		QanaryQuestion qanaryQuestion = mock(QanaryQuestion.class);

        URI ingraph = new URI("urn:qanary:ingraph");
        URI uri = new URI("test_question_uri");
        
        when(qanaryMessage.getInGraph()).thenReturn(ingraph);
        when(qanaryQuestion.getUri()).thenReturn(uri);
		
        // when the insert query is created
		String sparqlInsert = qb.getInsertQuery(qanaryMessage, qanaryQuestion, testQuery);

        logger.info(testQuery);

        // then it contains the created dbpediaQuery, the question graph and question URI
        assertTrue(sparqlInsert.contains("GRAPH <"+ingraph.toString()+">"));
        assertTrue(sparqlInsert.contains("oa:hasTarget <"+uri.toString()+">"));
        assertTrue(sparqlInsert.contains("oa:hasBody \""+testQuery.replace("\"", "\\\"").replace("\n", "\\n")+"\"^^xsd:string"));
	}
}
