package eu.wdaqua.component.querybuilder;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
public class SuperheroRealNameQueryBuilder extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SuperheroRealNameQueryBuilder.class);

	private final String applicationName;

	public SuperheroRealNameQueryBuilder(@Value("$P{spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
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
		
		// STEP 1: get the required data
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);

		// STEP 2: compute new knowledge about the given question

		// only continue if the question contains the supported phrase
		// "real name of"
		String supportedQuestionPrefix = "real name of ";
		if (!myQuestion.toLowerCase().contains(supportedQuestionPrefix)) {
			logger.info("nothing to do here as question \"{}\" is not starting with \"{}\".", myQuestion,
					supportedQuestionPrefix);
			return myQanaryMessage;
		}

		// look for annotations made by NED OpenTapioca component
		
		String sparqlGetAnnotation = "" //
				+ "PREFIX dbr: <http://dbpedia.org/resource/> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " //
				+ "SELECT * " //
				+ "FROM <" + myQanaryMessage.getInGraph().toString() + "> " //
				+ "WHERE { " //
				+ "    ?annotation     oa:hasBody   ?wikidataResource ." //
				+ "    ?annotation     qa:score     ?annotationScore ." //
				+ "    ?annotation     oa:hasTarget ?target ." //
				+ "    ?target     oa:hasSource    <" + myQanaryQuestion.getUri().toString() + "> ." //
				+ "    ?target     oa:hasSelector  ?textSelector ." //
				+ "    ?textSelector   rdf:type    oa:TextPositionSelector ." //
				+ "    ?textSelector   oa:start    ?start ." //
				+ "    ?textSelector   oa:end      ?end ." //
				+ "    FILTER(?start = " + supportedQuestionPrefix.length() + ") ." //
				+ "}";

		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparqlGetAnnotation);
		while(resultset.hasNext()) {
			QuerySolution tupel = resultset.next();
			int start = tupel.get("start").asLiteral().getInt();
			int end = tupel.get("end").asLiteral().getInt();
			String wikidataResource = tupel.get("wikidataResource").toString();
			logger.info("found matching resource <{}> at ({}, {})", wikidataResource, start, end);

			String createdWikiDataQuery = "" //
				+ "PREFIX wikibase: <http://wikiba.se/ontology#>" //
				+ "PREFIX bd: <http://www.bigdata.com/rdf#>" //
				+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>" //
				+ "PREFIX wd: <http://www.wikidata.org/entity/>" //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" //
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" //
				+ "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>" //
				+ "PREFIX ps: <http://www.wikidata.org/prop/statement/>" //
				+ "PREFIX p: <http://www.wikidata.org/prop/> " //
				+ "SELECT DISTINCT ?superhero ?name ?firstnameLabel ?familynameLabel ?region WHERE {" //
				+ "  VALUES ?superhero {<" + wikidataResource + ">}" //
				+ "  VALUES ?superheroTypes { wd:Q63998451 wd:Q188784 } " //
				+ "  VALUES ?allowedPropFirstname { pq:P518 pq:P642 }" //
				+ "  VALUES ?allowedPropFamilyname { pq:P518 pq:P642 }" //
				+ "  ?superhero rdfs:label ?name ." //
				+ "  FILTER(LANG(?name) = \"en\")" //
				+ "  {" //
				+ "    ?superhero wdt:P735 ?firstname ." //
				+ "    ?superhero wdt:P734 ?familyname ." //
				+ "  }" //
				+ "  UNION" //  
				+ "  { " //
				+ "    ?superhero wdt:P735 ?firstname ." //
				+ "    ?superhero p:P735 _:a . " //
				+ "    _:a ps:P735 ?firstname ." //
				+ "    _:a ?allowedPropFirstname ?region ." //
				+ "    ?superhero wdt:P734 ?familyname ." //
				+ "    ?superhero p:P734 _:b . " //
				+ "    _:b ps:P734 ?familyname ." //
				+ "    _:b ?allowedPropFamilyname ?region ." //
				+ "  }" //
				+ "  UNION" // only for superman
				+ "  {" //
				+ "    ?superhero wdt:P735 ?firstname ." //
				+ "    ?superhero p:P735 _:c . " //
				+ "    _:c ps:P735 ?firstname ." //
				+ "    _:c ?allowedPropFirstname ?region" //
				+ "" //
				+ "    FILTER NOT EXISTS { " //
				+ "    ?superhero wdt:P734 ?familyname ." //
				+ "    ?superhero p:P734 _:d . " //
				+ "    _:d ps:P734 ?familyname ." //
				+ "    _:d ?allowedPropFamilyname ?region ." //
				+ "    }" //
				+ "  }" //
				+ "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" } " //
				+ "}";
			
			// store the created SPARQL select query (which should compute the answer) into
			// the Qanary triplestore
			String insertDataIntoQanaryTriplestoreQuery = "" //
					+ "PREFIX dbr: <http://dbpedia.org/resource/>" //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" //
					+ "" //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryMessage.getInGraph().toString() + ">  {" //
					+ "        ?newAnnotation rdf:type qa:AnnotationOfAnswerSPARQL ." //
					+ "        ?newAnnotation oa:hasTarget <" + myQanaryQuestion.getUri().toString() + "> ." //
					+ "        ?newAnnotation oa:hasBody \""
					+ createdWikiDataQuery.replace("\"", "\\\"").replace("\n", "\\n") + "\"^^xsd:string ." //
					// as it is rule based, a high confidence is expressed
					+ "        ?newAnnotation qa:score \"1.0\"^^xsd:float ."
					+ "        ?newAnnotation oa:annotatedAt ?time ." //
					+ "        ?newAnnotation oa:annotatedBy <urn:qanary:"+this.applicationName+"> ." //
					+ "    }" //
					+ "}" //
					+ "WHERE {" //
					+ "    BIND (IRI(str(RAND())) AS ?newAnnotation) ." //
					+ "    BIND (now() as ?time) . " //
					+ "}";

			logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
					myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
					myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
			// push data to the Qanary triplestore
			myQanaryUtils.updateTripleStore(insertDataIntoQanaryTriplestoreQuery, myQanaryMessage.getEndpoint());

		}
		return myQanaryMessage;
	}
}
