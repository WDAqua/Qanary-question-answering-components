package eu.wdaqua.queryexecuter;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.io.ByteArrayOutputStream;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import io.swagger.v3.oas.annotations.Operation;

/**
 * represents a query executer for Wikidata
 *
 * requirements: 
 */

@Component
public class QueryExecuter extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QueryExecuter.class);

	private final String applicationName;

	public QueryExecuter(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Perform a POST request with the provided query to the Wikidata endpoint
	 *
	 * @param queryString the Wikidata query
	 * @return answerJson the response as JSON
	 */
	@Operation(
		summary = "Query Wikidata endpoint", //
		operationId = "getAnswersFromWikidata", //
		description = "Perform a POST request with the provided query to the Wikidata endpoint" //
	)
	public String getAnswersFromWikidata(String queryString) {

		String wikidataEndpoint = "https://query.wikidata.org/sparql";
		QueryExecution qexec = QueryExecutionFactory.sparqlService(wikidataEndpoint, queryString.replace("\\\"", "\"").replace("\\n", "\n"));

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
	 * @param answerJson the JSON returned by Wikidata
	 * @throws QanaryExceptionNoOrMultipleQuestions
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 * @return sparql 
	 */
	@Operation(
		summary = "Create a SPARQL insert query", //
		operationId = "getSparqlInsertQuery", //
		description = "Create a query to store the computed information in the Qanary triplestore"
	)
	public String getSparqlInsertQuery(QanaryQuestion myQanaryQuestion, String answerJson) 
			throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {		
		
		answerJson.replace("\"", "\\\"").replace("\n", "\\n");
		String sparql = "" //
					+ "PREFIX dbr: <http://dbpedia.org/resource/> \n" //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" //
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" //
					+ "" //
					+ "INSERT { \n" //
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + ">  {\n" //
					+ "  ?annotationAnswer a	qa:AnnotationOfAnswerJson ; \n" //
					+ "			oa:hasTarget	?question ; \n" //
					+ "			oa:hasBody		?answer ; \n" //
					+ "			oa:annotatedBy	?service ; \n" //
					+ "			oa:annotatedAt	?time ; \n" //
					+ "			qa:score		?score . \n" //
					//
					+ "  ?answer	a			qa:AnswerJson ;\n" //
					+ "	   rdf:value			?answerJson . \n" // the answer
					//?
					+ "  qa:AnswerJson rdfs:subClassOf qa:Answer . \n " //
					//
					+ " }\n" // end: graph
					+ "}\n" // end: insert
					+ "WHERE { \n" //
					+ "  BIND (IRI(str(RAND())) AS ?annotationAnswer) . \n" //
					+ "  BIND (IRI(str(RAND())) AS ?answer) . \n" //
					// values
					+ "  BIND (now() AS ?time) . \n" //
					+ "  BIND (<"+myQanaryQuestion.getUri().toASCIIString()+"> AS ?question) . \n" //
					+ "  BIND (\""+answerJson.replace("\"", "\\\"").replace("\n", "\\n")+"\"^^xsd:string AS ?answerJson) . \n" //
					+ "  BIND (\"1.0\"^^xsd:float AS ?score) . \n" // rule based
					+ "  BIND (<urn:qanary:"+this.applicationName+"> AS ?service) . \n" // use this.applicationName
					+"}\n";
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
	@Operation(
		summary = "Process a Qanary question with QueryExecuter", //
		operationId = "process", //
		description = "Encapsulates the main functionality of this component. "
					+ "Make a POST request to Wikidata using the computed queries to answer the" //
					+ "question and store the result as an annotation in the Qanary triplestore."//
	)
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// STEP 1: get the required data 
		// 
		// This example component will execute Wikidata queries that were created and stored
		// by previous components.

		// if required, then fetch the origin question (here the question is a
		// textual/String question)
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);

		// Here we fetch those annotations (AnnotationOfAnswerSPARQL) from the Qanary triplestore
		String sparqlSelectQuery = "" //
				+ "PREFIX dbr: <http://dbpedia.org/resource/> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " //
				+ "SELECT * " // 
				+ "FROM <" + myQanaryMessage.getInGraph().toString() + "> " // the currently used graph
				+ "WHERE { " //
				+ "	   ?annotation		a			qa:AnnotationOfAnswerSPARQL ." //
				+ "    ?annotation     oa:hasBody   ?wikidataQuery ." // the entity in question
				+ "    ?annotation     qa:score     ?annotationScore ." //
				+ "    ?annotation     oa:hasTarget <" + myQanaryQuestion.getUri().toString() + ">" //
				+ "}";

		// query the triplestore
		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparqlSelectQuery);
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
			if (isAnswerValid(answers)){
				// create an insert query to store new information
				String sparql = this.getSparqlInsertQuery(myQanaryQuestion, answers);
			
				// STEP 3: Push the computed knowledge about the given question to the Qanary triplestore
				
				logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
						myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
						myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
				
				// update the Qanary triplestore withthe created insert query
				myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint());
			}
		}
		return myQanaryMessage;
	}
}
