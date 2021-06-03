package eu.wdaqua.queryexecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.io.ByteArrayOutputStream;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

import org.json.simple.JSONObject;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class QueryExecutor extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

	// retrieve Json String Wikidata query results
	public String getAnswersFromWikidata(String queryString) {

		String wikidataEndpoint = "https://query.wikidata.org/sparql";
		List<JSONObject> answers = new LinkedList<JSONObject>();

		logger.info("querying wikidata endpoint with query:\n{}", queryString.replace("\\\"", "\"").replace("\\n", "\n"));

		Query query = QueryFactory.create(queryString.replace("\\\"", "\"").replace("\\n", "\n"));
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
					+ "  ?annotationAnswer a	qa:AnnotationAnswer ; \n" //
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
					+ "  BIND (<urn:qanary:qe-wikidata> AS ?service) . \n" // use this.applicationName
					+"}\n";
		return sparql;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component, some helping notes w.r.t. the typical 3 steps of implementing a
	 * Qanary component are included in the method (you might remove all of them)
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);

		
		
		// STEP 1: get the required data from the Qanary triplestore (the global process
		// memory)

		// if required, then fetch the origin question (here the question is a
		// textual/String question)
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);

		// define the SPARQL query here to fetch the data that your component is
		// requiring
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
				+ "    ?annotation     oa:hasTarget ?target ." //
				//+ "    ?target     oa:hasSource    <" + myQanaryQuestion.getUri().toString() + "> ." // annotated for the current question TODO:re-anable after checking qb query
				+ "}";

		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparqlSelectQuery);
		List<String> queries = new LinkedList<String>();
		while (resultset.hasNext()) {
			QuerySolution tupel = resultset.next();
			
			String wikidataQuery = tupel.get("wikidataQuery").toString();
			logger.info("found query {}", wikidataQuery);
			queries.add(wikidataQuery);
			// TODO: retrieve the data you need to implement your component's functionality
			// is start/end (surface form) required?
		}

		// STEP 2: compute new knowledge about the given question
		for (String queryString : queries) { //TODO: we can expect only one query per linked entity -> connect?
			String answers = this.getAnswersFromWikidata(queryString);
			String sparql = this.getSparqlInsertQuery(myQanaryQuestion, answers);
		
			// STEP 3: store computed knowledge about the given question into the Qanary
			// triplestore (the global process memory)
			
			// TODO: individual or compound query?

			logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
					myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
					myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
			// push data to the Qanary triplestore
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint());

		}
		return myQanaryMessage;
	}
}
