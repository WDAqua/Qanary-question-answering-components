package eu.wdaqua.qanary.tebaqa_wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.tebaqa_wrapper.messages.TeBaQAResult;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
@ComponentScan("eu.wdaqua.qanary")
public class TeBaQAQueryBuilderAndExecutorTest {

	QanaryTripleStoreConnectorInMemory myQanaryTripleStoreConnectorInMemory;

	@Autowired
	TeBaQAQueryBuilderAndExecutor myComponent;

	@Mock
	QanaryQuestion<String> myQanaryQuestionMock;

	@Mock
	TeBaQAResult myResultMock;

	/**
	 * test if inserts are working using templates from qanary.commons
	 * 
	 * @throws URISyntaxException
	 * @throws QanaryExceptionNoOrMultipleQuestions
	 * @throws SparqlQueryFailed
	 * @throws IOException
	 */
	@Test
	public void testInsert()
			throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, SparqlQueryFailed, IOException {
		myQanaryTripleStoreConnectorInMemory = new QanaryTripleStoreConnectorInMemory();
		URI outGraph = new URI("urn:outgraph");
		URI question = new URI("urn:question");
		String dummySparql = "SELECT * { ?s ?p ?o . }";
		double dummyConfidence = 0.5f;

		assertNotNull(myQanaryQuestionMock);
		assertNotNull(myQanaryTripleStoreConnectorInMemory);

		when(myQanaryQuestionMock.getOutGraph()).thenReturn(outGraph);
		when(myQanaryQuestionMock.getUri()).thenReturn(question);
		when(myResultMock.getSparql()).thenReturn(dummySparql);
		when(myResultMock.getConfidence()).thenReturn(dummyConfidence);

		myQanaryTripleStoreConnectorInMemory.connect(); // reset data
		// there should be no such annotation in the triplestore
		assertEquals(0, getAllAnnotationOfAnswerSPARQL(null).size());
		String insertSparqlQuery = myComponent.getSparqlInsertQuery(myQanaryQuestionMock, myResultMock);
		myQanaryTripleStoreConnectorInMemory.update(insertSparqlQuery);

		// ask for all AnnotationOfAnswerSPARQL without restrictions
		// there should be 1 such annotation in the triplestore
		assertEquals(1, getAllAnnotationOfAnswerSPARQL(null).size());

		// ask for all AnnotationOfAnswerSPARQL with exactly the same restrictions as in
		// the INSERT query
		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("graph", ResourceFactory.createResource(myQanaryQuestionMock.getOutGraph().toASCIIString()));
		bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestionMock.getUri().toASCIIString()));
		bindings.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createStringLiteral(dummySparql));
		bindings.add("confidence", ResourceFactory.createTypedLiteral(dummyConfidence));
		bindings.add("application", ResourceFactory.createResource("urn:qanary:" + myComponent.applicationName));
		// there should be 1 such annotation in the triplestore
		assertEquals(1, getAllAnnotationOfAnswerSPARQL(bindings).size());

	}

	private List<QuerySolution> getAllAnnotationOfAnswerSPARQL(QuerySolutionMap bindings)
			throws SparqlQueryFailed, IOException {
		String selectAllAnnotationOfAnswerSPARQL = QanaryTripleStoreConnector.getAnnotationOfAnswerSPARQL(bindings);
		List<QuerySolution> allAnnotations = new LinkedList<>();
		ResultSet result = myQanaryTripleStoreConnectorInMemory.select(selectAllAnnotationOfAnswerSPARQL);
		while (result.hasNext()) {
			allAnnotations.add(result.nextSolution());
		}
		return allAnnotations;
	}

}