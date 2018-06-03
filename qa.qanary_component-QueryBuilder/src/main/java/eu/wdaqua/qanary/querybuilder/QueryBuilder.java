package eu.wdaqua.qanary.querybuilder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

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

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 *
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		String detectedPattern = "";
String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
		List<String> classes = new ArrayList<String>();
		List<String> properties = new ArrayList<String>();
		List<String> entities = new ArrayList<String>();
		String graph = "<http://dbpedia.org>";
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		// entities

		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?start ?end ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
				+ "		     a               oa:SpecificResource; " //
				+ "		     oa:hasSource    ?q; " //
				+ "	         oa:hasSelector  [ " //
				+ "			         a        oa:TextPositionSelector ; " //
				+ "			         oa:start ?start ; " //
				+ "			         oa:end   ?end " //
				+ "		     ] " //
				+ "    ] . " //
				+ " ?a oa:hasBody ?uri . " + "} " + "ORDER BY ?start ";

		ResultSet r = myQanaryUtils.selectFromTripleStore(sparql);
		String argument = "";
		while (r.hasNext()) {
			QuerySolution s = r.next();

			entities.add(s.getResource("uri").getURI());
			logger.info("uri info {}", s.getResource("uri").getURI());
		}

		// property
		sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT  ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfRelation . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
				+ "           oa:hasSource    ?q; " + "  ]; " + "     oa:hasBody ?uri ;}";

	//	r = myQanaryUtils.selectFromTripleStore(sparql);
	r = myQanaryUtils.selectFromTripleStore(sparql, endpoint);

		while (r.hasNext()) {
			QuerySolution s = r.next();
			properties.add(s.getResource("uri").getURI());
			logger.info("uri info {}", s.getResource("uri").getURI());
		}

		// classes
		/*sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?relationurl " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfClass . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
				+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
				// + " oa:start ?start; " //
				// + " oa:end ?end " //
				+ "  ] ; " + "     oa:hasBody ?relationurl ;"
				// + " oa:annotatedBy <; "
				+ "	    oa:AnnotatedAt ?time  " + "} " //
				+ "ORDER BY ?start ";*/
		sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT  ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfClass . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
				+ "           oa:hasSource    ?q; " + "  ]; " + "     oa:hasBody ?uri ;}";

	//	r = myQanaryUtils.selectFromTripleStore(sparql);
	r = myQanaryUtils.selectFromTripleStore(sparql, endpoint);

		while (r.hasNext()) {
			QuerySolution s = r.next();
			classes.add(s.getResource("uri").getURI());
			logger.info("uri info {}", s.getResource("uri").getURI());
		}

		String generatedQuery = "";
		if (classes.size() == 0) {

			if (properties.size() == 1) {
				if (entities.size() == 1) {

					if (myQuestion.contains("Is") || myQuestion.contains("Did") || myQuestion.contains("do")
							|| myQuestion.contains("Does") || myQuestion.contains("Was") || myQuestion.contains("Were")
							|| myQuestion.contains("Are")) {
						generatedQuery = "ASK WHERE { <" + entities.get(0) + "> <" + properties.get(0) + "> ?v1. }";
					} else if (myQuestion.contains("How") || myQuestion.contains("many")) {

						generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE{ <" + entities.get(0) + "> <"
								+ properties.get(0) + "> ?v1. }";

					} else {

						generatedQuery = "SELECT DISTINCT ?uri WHERE { <" + entities.get(0) + "> <" + properties.get(0)
								+ "> ?uri }";
					}
				}
				if (entities.size() == 2) {

					if (myQuestion.contains("Is") || myQuestion.contains("Did") || myQuestion.contains("do")
							|| myQuestion.contains("Does") || myQuestion.contains("Was") || myQuestion.contains("Were")
							|| myQuestion.contains("Are")) {
							generatedQuery = "ASK WHERE { <" + entities.get(0) + "> <" + properties.get(0) + "> <"
								+ entities.get(1) + "> }";
					}
					if (myQuestion.contains("both")||myQuestion.contains("common")||myQuestion.contains("also")) {

						generatedQuery = "SELECT DISTINCT ?uri WHERE { ?uri <" + properties.get(0) + "> <" + entities.get(0)
						+ "> . ?uri <" + properties.get(0) + "> <" + entities.get(1) + "> . }";

					}

				}
			} else if (properties.size() == 2) {
				if (entities.size() == 1) {

					if (myQuestion.contains("How") || myQuestion.contains("many")) {

						generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE { ?x <" + properties.get(0) + "> <"
								+ entities.get(0) + "> . ?x <" + properties.get(1) + "> ?uri . }";

						Query query = QueryFactory.create(generatedQuery);
						QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
						ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
						if (!results.hasNext()) {
							generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE { <" + entities.get(0) + "> <"
									+ properties.get(0) + "> ?x. ?x <" + properties.get(1) + "> ?uri  }";
						}

					} else {
						generatedQuery = "SELECT DISTINCT ?uri WHERE { ?x <" + properties.get(0) + "> <"
								+ entities.get(0) + "> . ?x <" + properties.get(1) + "> ?uri . }";

						Query query = QueryFactory.create(generatedQuery);
						QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
						ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
						if (!results.hasNext()) {
							generatedQuery = "SELECT DISTINCT ?uri WHERE { <" + entities.get(0) + "> <"
									+ properties.get(0) + "> ?x. ?x <" + properties.get(1) + "> ?uri  }";
						}
					}
				}
				if (entities.size() == 2) {

					generatedQuery = "SELECT DISTINCT ?uri WHERE { ?uri <" + properties.get(0) + "> <" + entities.get(0)
							+ "> . ?uri <" + properties.get(1) + "> <" + entities.get(1) + "> . }";
				}
			}

		} else if (classes.size() == 1) {

			if (properties.size() == 0) {
				if (entities.size() == 1) {

				}
				if (entities.size() == 2) {

				}
			} else if (properties.size() == 1) {
				if (entities.size() == 1) {

					generatedQuery = "SELECT DISTINCT ?uri WHERE {?uri <" + properties.get(0) + "> <" + entities.get(0)
							+ "> . ?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + ">}";

					Query query = QueryFactory.create(generatedQuery);
					QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
					ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
					if (!results.hasNext()) {
						generatedQuery = "SELECT DISTINCT ?uri WHERE { <" + entities.get(0) + "> <" + properties.get(0)
								+ "> ?uri. ?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0)
								+ ">}";
					}

				}
				if (entities.size() == 2) {

				}
			} else if (properties.size() == 2) {
				if (entities.size() == 1) {
					generatedQuery = "SELECT DISTINCT ?uri WHERE { ?x <" + properties.get(0) + "> <" + entities.get(0)
							+ "> . ?x <" + properties.get(1)
							+ "> ?uri . ?x <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0)
							+ ">}";
				}
				if (entities.size() == 2) {
					generatedQuery = "SELECT DISTINCT ?uri WHERE {?uri <" + properties.get(0) + "> <" + entities.get(0)
							+ "> . ?uri <" + properties.get(1) + "> <" + entities.get(1)
							+ "> . ?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + ">}";
				}
			}

		} else if (classes.size() == 2) {

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





		logger.debug("store the generated GeoSPARQL query in triplestore: {}", generatedQuery);
		// STEP 3: Push the GeoSPARQL query to the triplestore
		if (generatedQuery != "") {
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryUtils.getInGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfAnswerSPARQL . " //
					+ "  ?a oa:hasTarget <URIAnswer> . " //
					+ "  ?a oa:hasBody \"" + generatedQuery.replaceAll("\n", " ") + "\" ;" //
					+ "     oa:annotatedBy <urn:qanary:geosparqlgenerator> ; " //
					+ "	    oa:AnnotatedAt ?time . " //
					+ "}} " //
					+ "WHERE { " //
					+ "	BIND (IRI(str(RAND())) AS ?a) ." //
					+ "	BIND (now() as ?time) " //
					+ "}";
		//	myQanaryUtils.updateTripleStore(sparql);
		myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());

			Query query = QueryFactory.create(generatedQuery);
			QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
			ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			ResultSetFormatter.outputAsJSON(outputStream, results);
			String json = new String(outputStream.toByteArray(), "UTF-8");

			logger.info("apply vocabulary alignment on outgraph");
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
	                	+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
	                	+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
	                	+ "INSERT { "
	                	+ "GRAPH <" + myQanaryUtils.getOutGraph() + "> { "
	                	+ "  ?b a qa:AnnotationOfAnswerJSON . "
	                	+ "  ?b oa:hasTarget <URIAnswer> . "
	                	+ "  ?b oa:hasBody \"" + json.replace("\n", " ").replace("\"", "\\\"") + "\" ;"
	                	+ "     oa:annotatedBy <www.wdaqua.eu> ; "
	                	+ "         oa:annotatedAt ?time  "
	                	+ "}} "
	                	+ "WHERE { "
	                	+ "  BIND (IRI(str(RAND())) AS ?b) ."
	                	+ "  BIND (now() as ?time) "
	                	+ "}";
	        	//myQanaryUtils.updateTripleStore(sparql);
						myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());

		}
		return myQanaryMessage;
	}

	class Entity {

		public int begin;
		public int end;
		public String namedEntity;
		public String uri;

		public void print() {
			System.out.println("Start: " + begin + "\t End: " + end + "\t Entity: " + namedEntity);
		}
	}
}
