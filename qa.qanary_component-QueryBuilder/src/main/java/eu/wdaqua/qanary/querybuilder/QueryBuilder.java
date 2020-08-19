package eu.wdaqua.qanary.querybuilder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class QueryBuilder extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);

	private final String applicationName;

	public QueryBuilder(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 *
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		String dbpediaSparqEndpoint = "http://dbpedia.org/sparql";
		String sparql;
		
		// random answer URI
		String answerID = "urn:qanary:answer:" + UUID.randomUUID().toString(); 

		// get entities, properties, classes from current question
		List<String> entities = getEntitiesFromQanaryKB(myQanaryUtils, myQanaryQuestion);
		List<String> properties = getPropertiesFromQanaryKB(myQanaryUtils, myQanaryQuestion);
		List<String> classes = getClassesFromQanaryKB(myQanaryUtils, myQanaryQuestion);

		String generatedQuery = "";
		if (classes.size() == 0) {

			if (properties.size() == 1) {
				if (entities.size() == 1) {

					if (myQuestion.contains("Is") || myQuestion.contains("Did") || myQuestion.contains("do")
							|| myQuestion.contains("Does") || myQuestion.contains("Was") || myQuestion.contains("Were")
							|| myQuestion.contains("Are")) {
						generatedQuery = "ASK WHERE { " //
								+ "<" + entities.get(0) + "> <" + properties.get(0) + "> ?v1 . " //
								+ "}";
					} else if (myQuestion.contains("How") || myQuestion.contains("many")) {
						generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE{ " //
								+ "<" + entities.get(0) + "> <" + properties.get(0) + "> ?v1 . " //
								+ "}";

					} else {
						generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
								+ "<" + entities.get(0) + "> <" + properties.get(0) + "> ?uri ." //
								+ "}";
					}
				}
				if (entities.size() == 2) {

					if (myQuestion.contains("Is") || myQuestion.contains("Did") || myQuestion.contains("do")
							|| myQuestion.contains("Does") || myQuestion.contains("Was") || myQuestion.contains("Were")
							|| myQuestion.contains("Are")) {
						generatedQuery = "ASK WHERE { " //
								+ "<" + entities.get(0) + "> <" + properties.get(0) + "> <" + entities.get(1) + "> ." //
								+ "}";
					}
					if (myQuestion.contains("both") || myQuestion.contains("common") || myQuestion.contains("also")) {

						generatedQuery = "SELECT DISTINCT ?uri WHERE {"//
								+ " ?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
								+ " ?uri <" + properties.get(0) + "> <" + entities.get(1) + "> . " //
								+ "}";
					}
				}
			} else if (properties.size() == 2) {
				if (entities.size() == 1) {

					if (myQuestion.contains("How") || myQuestion.contains("many")) {

						generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE { " //
								+ "?x <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
								+ "?x <" + properties.get(1) + "> ?uri . " //
								+ "}";

						Query query = QueryFactory.create(generatedQuery);
						QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqEndpoint, query);
						ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

						if (!results.hasNext()) {
							generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE { " //
									+ "<" + entities.get(0) + "> <" + properties.get(0) + "> ?x . " //
									+ "?x <" + properties.get(1) + "> ?uri . " //
									+ "}";
						}

					} else {
						generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
								+ "?x <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
								+ "?x <" + properties.get(1) + "> ?uri . "//
								+ "}";

						Query query = QueryFactory.create(generatedQuery);
						QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqEndpoint, query);
						ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
						if (!results.hasNext()) {
							generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
									+ "<" + entities.get(0) + "> <" + properties.get(0) + "> ?x . " //
									+ "?x <" + properties.get(1) + "> ?uri . " //
									+ "}";
						}
					}
				}
				if (entities.size() == 2) {

					generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
							+ "?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
							+ "?uri <" + properties.get(1) + "> <" + entities.get(1) + "> . }";
				}
			}

		} else if (classes.size() == 1) {

			if (properties.size() == 0) {
				if (entities.size() == 1) {
					// TODO
				} else if (entities.size() == 2) {
					// TODO
				}
			} else if (properties.size() == 1) {
				if (entities.size() == 1) {

					generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
							+ "?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
							+ "?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
							+ "}";

					Query query = QueryFactory.create(generatedQuery);
					QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqEndpoint, query);
					ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
					if (!results.hasNext()) {
						generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
								+ "<" + entities.get(0) + "> <" + properties.get(0) + "> ?uri . " //
								+ "?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
								+ "}";
					}

				}
				if (entities.size() == 2) {
					// TODO
				}
			} else if (properties.size() == 2) {
				if (entities.size() == 1) {
					generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
							+ "?x <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
							+ "?x <" + properties.get(1) + "> ?uri . " //
							+ "?x <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
							+ "}";
				}
				if (entities.size() == 2) {
					generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
							+ "?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
							+ "?uri <" + properties.get(1) + "> <" + entities.get(1) + "> . " //
							+ "?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
							+ "}";
				}
			}

		} else if (classes.size() == 2) { // TODO

			if (properties.size() == 0) {
				if (entities.size() == 1) {

				}
				if (entities.size() == 2) {

				}
			} else if (properties.size() == 1) {
				if (entities.size() == 1) {

				}
				if (entities.size() == 2) {

				}
			} else if (properties.size() == 2) {
				if (entities.size() == 1) {

				}
				if (entities.size() == 2) {

				}
			}
		}

		logger.debug("store the generated SPARQL query in triplestore: {}", generatedQuery);
		// STEP 3: Push the SPARQL query to the triplestore
		if (generatedQuery != "") {
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryUtils.getOutGraph() + "> { " //
					+ "  ?answer a qa:Answer . " // 
					+ "  ?a a qa:AnnotationOfAnswerSPARQL . " //
					+ "  ?a oa:hasTarget ?answer . " //
					+ "  ?a oa:hasBody \"" + generatedQuery.replaceAll("\n", " ") + "\" ;" //
					+ "     oa:annotatedBy <urn:qanary:QB#" + this.applicationName + "> ; " //
					+ "	    oa:annotatedAt ?time . " //
					+ "}} " //
					+ "WHERE { " //
					+ "	BIND (IRI(str(RAND())) AS ?a) ." //
					+ "	BIND (now() as ?time) . " //
					+ " BIND (<" + answerID + "> as ?answer) ." //
					+ "}";
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());

			Query query = QueryFactory.create(generatedQuery);
			QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqEndpoint, query);

			ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			ResultSetFormatter.outputAsJSON(outputStream, results);
			String json = new String(outputStream.toByteArray(), "UTF-8");

			logger.info("Push the the JSON object to the named graph reserved for the answer.");
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryUtils.getOutGraph() + "> { " //
					+ "  ?answer a qa:Answer . " // 
					+ "  ?b a qa:AnnotationOfAnswerJSON ; " //
					+ "     oa:hasTarget <" + answerID + "> ; " //
					+ "     oa:hasBody \"" + json.replace("\n", " ").replace("\"", "\\\"") + "\" ;" //
					+ "     oa:annotatedBy <urn:qanary:QB#" + QueryBuilder.class.getName() + "> ; " //
					+ "     oa:annotatedAt ?time  " //
					+ "}} " //
					+ "WHERE { " //
					+ "  BIND (IRI(str(RAND())) AS ?b) ." //
					+ "  BIND (now() as ?time) " //
					+ "  BIND (<" + answerID + "> as ?answer) ." //
					+ "}";
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());

		}
		return myQanaryMessage;
	}

	/**
	 * get all annotated classes of the user's question from the Qanary
	 * Knowledge Base / Triplestore
	 * 
	 * @param myQanaryUtils
	 * @param myQanaryQuestion
	 * @return
	 */
	private List<String> getClassesFromQanaryKB(QanaryUtils myQanaryUtils, QanaryQuestion<String> myQanaryQuestion) throws SparqlQueryFailed {
		List<String> classes = new ArrayList<String>();
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "SELECT  ?uri " //
				+ "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfClass . " //
				+ "  ?a oa:hasTarget [ " //
				+ "       a            oa:SpecificResource; " //
				+ "       oa:hasSource ?q; " //
				+ "     ]; " //
				+ "     oa:hasBody ?uri ; }";

		ResultSet r = myQanaryUtils.selectFromTripleStore(sparql);

		while (r.hasNext()) {
			QuerySolution s = r.next();
			classes.add(s.getResource("uri").getURI());
			logger.info("class: {}", s.getResource("uri").getURI());
		}

		return classes;
	}

	/**
	 * get all annotated properties of the user's question from the Qanary
	 * Knowledge Base / Triplestore
	 * 
	 * @param myQanaryUtils
	 * @param myQanaryQuestion
	 * @return
	 */
	private List<String> getPropertiesFromQanaryKB(QanaryUtils myQanaryUtils, QanaryQuestion<String> myQanaryQuestion) throws SparqlQueryFailed {
		List<String> properties = new ArrayList<String>();

		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT  ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfRelation . " //
				+ "  ?a oa:hasTarget [ " //
				+ "        a oa:SpecificResource; " //
				+ "        oa:hasSource    ?q; " //
				+ "     ]; " //
				+ "     oa:hasBody ?uri ; }";

		ResultSet r = myQanaryUtils.selectFromTripleStore(sparql);

		while (r.hasNext()) {
			QuerySolution s = r.next();
			properties.add(s.getResource("uri").getURI());
			logger.info("property: {}", s.getResource("uri").getURI());
		}
		return properties;
	}

	/**
	 * get all annotated entities of the user's question from the Qanary
	 * Knowledge Base / Triplestore
	 * 
	 * @param myQanaryUtils
	 * @param myQanaryQuestion
	 * @return
	 */
	private List<String> getEntitiesFromQanaryKB(QanaryUtils myQanaryUtils, QanaryQuestion<String> myQanaryQuestion) throws SparqlQueryFailed {
		List<String> entities = new ArrayList<String>();

		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?start ?end ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ " //
				+ "		     a               oa:SpecificResource; " //
				+ "		     oa:hasSource    ?q; " //
				+ "	         oa:hasSelector  [ " //
				+ "			         a        oa:TextPositionSelector ; " //
				+ "			         oa:start ?start ; " //
				+ "			         oa:end   ?end " //
				+ "		     ] " //
				+ "    ] . " //
				+ " ?a oa:hasBody ?uri . " + "} " //
				+ "ORDER BY ?start ";

		ResultSet r = myQanaryUtils.selectFromTripleStore(sparql);
		while (r.hasNext()) {
			QuerySolution s = r.next();

			entities.add(s.getResource("uri").getURI());
			logger.info("entity: {}", s.getResource("uri").getURI());
		}
		return entities;
	}

}
