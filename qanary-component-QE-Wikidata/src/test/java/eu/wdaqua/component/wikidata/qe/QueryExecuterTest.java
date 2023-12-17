package eu.wdaqua.component.wikidata.qe;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * test the correct functionality of the SPARQL INSERT query used in
 * QueryExecuter Qanary component
 *
 * @author AnBo
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class QueryExecuterTest {
	private static final Logger logger = LoggerFactory.getLogger(QueryExecuterTest.class);

	@Autowired
	QueryExecuter myQueryExecuter;

	@Test
	void testTheNumberOfTriplesGeneratedByQueryExecutor()
			throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, IOException {
		URI question = new URI("urn:question");
		URI outGraph = new URI("urn:outgraph");

		QanaryTripleStoreConnectorInMemory myQanaryTripleStoreConnector = new QanaryTripleStoreConnectorInMemory();

		String sparqlInsertQuery = myQueryExecuter.getSparqlInsertQuery(outGraph, question, "{}");
		logger.info("sparqlInsertQuery:\n{}", sparqlInsertQuery);

		myQanaryTripleStoreConnector.update(sparqlInsertQuery);

		String sparqlQueryString = QanaryTripleStoreConnector.getCountAllTriplesInGraph(outGraph);
		ResultSet results = myQanaryTripleStoreConnector.select(sparqlQueryString);
		for (; results.hasNext(); ) {
			// there should be just one result and the number of triples should be 9
			assertEquals(9, results.nextSolution().getLiteral("count").getInt());
		}
	}

	@Test
	void testTheNumberOfAnnotationOfAnswerJsonGeneratedByQueryExecutor()
			throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, IOException {

		URI question = new URI("urn:question");
		URI outGraph = new URI("urn:outgraph");

		QanaryTripleStoreConnectorInMemory myQanaryTripleStoreConnector = new QanaryTripleStoreConnectorInMemory();

		String sparqlInsertQuery = myQueryExecuter.getSparqlInsertQuery(outGraph, question, "{}");
		logger.info("sparqlInsertQuery:\n{}", sparqlInsertQuery);

		myQanaryTripleStoreConnector.update(sparqlInsertQuery);

		String sparqlQueryString = QanaryTripleStoreConnector.getAllAnnotationOfAnswerInGraph(outGraph);
		ResultSet results = myQanaryTripleStoreConnector.select(sparqlQueryString);
		int count = 0;
		for (; results.hasNext(); ) {
			QuerySolution solution = results.nextSolution();
			count = results.getRowNumber();
			logger.debug("{}. solution: {}", results.getRowNumber(), solution.toString());
			// check if target of the annotation is the assumed question URI
			assertEquals(question.toASCIIString(), solution.get("hasTarget").asResource().toString());
		}
		// check if the number of generated annotation is 1
		assertEquals(1, count, "There should be just 1 result after executing the component.");
	}

}
