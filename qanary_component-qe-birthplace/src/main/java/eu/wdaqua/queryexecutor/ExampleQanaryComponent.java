package eu.wdaqua.queryexecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

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

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class ExampleQanaryComponent extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(ExampleQanaryComponent.class);

	// TODO: return Json instead of String
	// TODO: individual queries
	public List<BirthData> getAnswersFromWikidata(List<String> queries) {

		String wikidataEndpoint = "https://query.wikidata.org/sparql";
		List<BirthData> answers = new LinkedList<BirthData>();

   		for (String queryString : queries) {
   			// TODO: execute queries
   			
   			Query query = QueryFactory.create(queryString);
   			QueryExecution qexec = QueryExecutionFactory.sparqlService(wikidataEndpoint, queryString);
   
   			try {
   				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution result = results.next();
					
					String personLabel = result.get("personLabel").toString();
					String birthplace = result.get("birthplace").toString();
					String birthdate = result.get("birthdate").toString();
					BirthData birthData = new BirthData(personLabel, birthplace, birthdate);

					logger.info("found: {}", answer);
					answers.add(birthData);
				}
   			} catch (Exception e) {
   				logger.warn("could not query wikidata endpoint {}", e.getMessage());
   			} finally {
   				qexec.close();
   			}
   		}
		return answers;
	}

	public String getSparqlInsertQuery() {
		String sparql = "";

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
				+ "    ?annotation     oa:hasBody   ?wikidataQuery ." // the entity in question
				+ "    ?annotation     qa:score     ?annotationScore ." //
				+ "    ?annotation     oa:hasTarget ?target ." //
				+ "    ?target     oa:hasSource    <" + myQanaryQuestion.getUri().toString() + "> ." // annotated for the current question
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
		List<BirthData> answers = this.getAnswersFromWikidata(queries);
		for (BirthData answer : answers) {
			String sparql = this.getSparqlInsertQuery(myQanaryQuestion, answer);	
		}

		
		
		// STEP 3: store computed knowledge about the given question into the Qanary
		// triplestore (the global process memory)
		
		// TODO: individual or compound query?

		logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
				myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
				myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// push data to the Qanary triplestore
		myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint());

		return myQanaryMessage;
	}
}
