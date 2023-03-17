package eu.wdaqua.component.wikidata.qe;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import io.swagger.v3.oas.annotations.Operation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

/**
 * represents a query executer for Wikidata
 *
 * requirements:
 */

@Component
public class QueryExecuter extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QueryExecuter.class);

	private final String applicationName;

	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";
	private String FILENAME_GET_ANNOTATION = "/queries/get_annotation.rq";

	public QueryExecuter(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_ANNOTATION);
	}

	/**
	 * Perform a POST request with the provided query to the Wikidata endpoint
	 *
	 * @param queryString the Wikidata query
	 * @return answerJson the response as JSON
	 */
	@Operation(summary = "Query Wikidata endpoint", //
			operationId = "getAnswersFromWikidata", //
			description = "Perform a POST request with the provided query to the Wikidata endpoint" //
	)
	public String getAnswersFromWikidata(String queryString) {

		String wikidataEndpoint = "https://query.wikidata.org/sparql";
		QueryExecution qexec = QueryExecutionFactory.sparqlService(wikidataEndpoint,
				queryString.replace("\\\"", "\"").replace("\\n", "\n"));

		try {
			ResultSet results = qexec.execSelect();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ResultSetFormatter.outputAsJSON(os, results);
			return os.toString("UTF-8");
		} catch (Exception e) {
			logger.warn("could not query wikidata endpoint {}", e.getMessage());
		} finally {
			qexec.close();
		}
		return null;
	}

	/**
	 * Create a query to store the computed information in the Qanary triplestore
	 *
	 * @param myQanaryQuestion the QanaryQuestion currently being processed
	 * @param answerJson       the JSON returned by Wikidata
	 * @return sparql
	 * @throws QanaryExceptionNoOrMultipleQuestions
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 */
	@Operation(summary = "Create a SPARQL insert query", //
			operationId = "getSparqlInsertQuery", //
			description = "Create a query to store the computed information in the Qanary triplestore")
	public String getSparqlInsertQuery(URI outGraph, URI question, String answerJson)
			throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

		answerJson = answerJson.replace("\"", "\\\"").replace("\n", "\\n");

		QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
		bindingsForInsert.add("graph", ResourceFactory.createResource(outGraph.toASCIIString()));
		bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(question.toASCIIString()));
		bindingsForInsert.add("answerJson", ResourceFactory.createTypedLiteral(answerJson, XSDDatatype.XSDstring));
		bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

		// get the template of the INSERT query
		String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
		return sparql;
	}

	public boolean isAnswerValid(String answerJson) {
		try {
			JSONObject obj = (JSONObject) JSONSerializer.toJSON(answerJson);
			JSONObject results = obj.getJSONObject("results");
			logger.info("results: {}", results.toString());
			JSONArray bindings = results.getJSONArray("bindings");
			logger.info("bindings: {}", bindings.toString());
			logger.info("size: {}", bindings.size());
			if (bindings.size() > 0)
				return true;
		} catch (Exception e) {
			logger.info("the provided JSON could not be parsed");
		}
		return false;
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 *
	 * @param myQanaryMessage
	 * @throws Exception
	 */
	@Operation(summary = "Process a Qanary question with QueryExecuter", //
			operationId = "process", //
			description = "Encapsulates the main functionality of this component. " //
					+ "Make a POST request to Wikidata using the computed queries to answer the" //
					+ "question and store the result as an annotation in the Qanary triplestore."//
	)
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// STEP 1: get the required data
		//
		// This example component will execute Wikidata queries that were created and
		// stored
		// by previous components.

		// if required, then fetch the origin question (here the question is a
		// textual/String question)
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage,
				myQanaryUtils.getQanaryTripleStoreConnector());

		// Here we fetch those annotations (AnnotationOfAnswerSPARQL) from the Qanary
		// triplestore

		QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
		bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));
		bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));

		// get the template of the INSERT query
		String sparqlSelectQuery = this.loadQueryFromFile(FILENAME_GET_ANNOTATION, bindingsForInsert);

		// fetch data from the triplestore
		ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlSelectQuery);
		List<String> queries = new LinkedList<String>();

		while (resultset.hasNext()) {
			QuerySolution tupel = resultset.next();

			String wikidataQuery = tupel.get("wikidataQuery").toString();
			logger.info("found query {}", wikidataQuery);
			queries.add(wikidataQuery);
		}

		// STEP 2: compute new knowledge about the given question
		//
		// Send a post request to the Wikidata endpoint using the queries that should
		// answer the question

		for (String queryString : queries) {
			// get the results from Wikidata
			String answers = this.getAnswersFromWikidata(queryString);
			if (isAnswerValid(answers)) {
				// create an insert query to store new information
				String sparql = this.getSparqlInsertQuery(myQanaryQuestion.getOutGraph(), myQanaryQuestion.getUri(), answers);

				// STEP 3: Push the computed knowledge about the given question to the Qanary
				// triplestore

				logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
						myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
						myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));

				// update the Qanary triplestore withthe created insert query
				myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
			}
		}
		return myQanaryMessage;
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}
}
