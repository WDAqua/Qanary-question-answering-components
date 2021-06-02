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
public class BirthplaceQueryBuilder extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BirthplaceQueryBuilder.class);

	private final String applicationName;

	private final String[] supportedQuestionSubstrings = {"birthplace of"};
	// currently only one substing, change may require better implementation in subsequent methods

	public BirthplaceQueryBuilder(@Value("$P{spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}

	// TODO: support more prefixes
	private boolean isQuestionSupported(String questionString) {
		return questionString.toLowerCase().contains(this.supportedQuestionSubstrings[0]);
	}

	// TODO: work with all prefixes
	private int getNamePosition(String questionString) {
		int foundSubstring = questionString.toLowerCase().indexOf(this.supportedQuestionSubstrings[0]);
		int filterStart = foundSubstring + this.supportedQuestionSubstrings[0].length();
		return filterStart;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary component
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		
		// STEP 1: Get the required Data
		//
		// This example component requires the textual representation of the Question 
		// as well as annotations of Wikidata entities made by the OpenTapioca NED.
		
		// get the question as String
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);


		// TODO: change according to combined functionality
		// This component is only supposed to answer a specific type of question.
		// Therefore we only need to continue if the question asks for a birthplace.
		// For this example is is enough to simply look for the substring "birthplace of".
		// However, a more sophisticated approach is very possible.

		if (!this.isQuestionSupported(myQuestion)) {
			// don't continue the process if the question is not supported
			logger.info("nothing to do here as question \"{}\" does not contain \"{}\".", myQuestion,
					this.supportedQuestionSubstrings[0]);
			return myQanaryMessage;
		}

		// STEP 2: Get Wikidata entities that were annotated by OpenTapioca NED.
		//
		// In this example we are only interested in Entities that were found after the 
		// supported substring: "what is the 'birthplace of '<name>?"
		// Because we do not require entities that were found before that substring we can 
		// filter our results:

		int filterStart = this.getNamePosition(myQuestion);
		// formulate a query to find existing information 
		String sparqlGetAnnotation = "" //
				+ "PREFIX dbr: <http://dbpedia.org/resource/> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " //
				+ "SELECT * " // 
				+ "FROM <" + myQanaryMessage.getInGraph().toString() + "> " // the currently used graph
				+ "WHERE { " //
				+ "    ?annotation     oa:hasBody   ?wikidataResource ." // the entity in question
				+ "    ?annotation     qa:score     ?annotationScore ." //
				+ "    ?annotation     oa:hasTarget ?target ." //
				+ "    ?target     oa:hasSource    <" + myQanaryQuestion.getUri().toString() + "> ." // annotated for the current question
				+ "    ?target     oa:hasSelector  ?textSelector ." //
				+ "    ?textSelector   rdf:type    oa:TextPositionSelector ." //
				+ "    ?textSelector   oa:start    ?start ." //
				+ "    ?textSelector   oa:end      ?end ." //
				+ "    FILTER(?start = " + filterStart + ") ." // only for relevant annotations
				+ "}";

		// STEP 3: Compute SPARQL select queries that should produce the result for every identified entity
		//
		// Rather than computing a (textual) result this component provides a
		// SPARQL query that might be used to answer the question.
		// This query can the used by other components. 

		// there might be multiple entities identified for one name
		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparqlGetAnnotation);
		while(resultset.hasNext()) {
			QuerySolution tupel = resultset.next();
			String wikidataResource = tupel.get("wikidataResource").toString();
			logger.info("creating query for resource: {}", wikidataResource);

			// populate a generalized answer query with the specific entity (wikidata ID)
			String createdWikiDataQuery = "" //
				+ "PREFIX wikibase: <http://wikiba.se/ontology#> " //
				+ "PREFIX wd: <http://www.wikidata.org/entity/> " //
				+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " //
				+ "PREFIX bd: <http://www.bigdata.com/rdf#> " //
				+ "PREFIX p: <http://www.wikidata.org/prop/> " //
				+ "PREFIX pq: <http://www.wikidata.org/prop/qualifier/> " //
				+ "PREFIX ps: <http://www.wikidata.org/prop/statement/> " //
				+ "select DISTINCT ?personLabel ?birthplaceLabel ?birthdate " //
				+ "where { " //
				+ "	 values ?allowedPropPlace { pq:P17 } " // allow 'country' as property of birthplace
				+ "  values ?person {<"+wikidataResource+">} " //
				+ "  ?person wdt:P569 ?birthdate . " // this should produce the date of birth 
				+ "  {" //
				+ "  ?person wdt:P19 ?birthplace . " // this should produce the place of birth
				+ "  }" //
				+ "	 UNION" //
				+ "  {" //
				+ "  ?person wdt:P19 ?birthplace . " // 
				+ "  ?person p:P19 _:a . " //			
				+ "  _:a ps:P19 ?specificBirthPlace . " // the above place might be too specific
				+ "  _:a ?allowedPropPlace ?birthplace . "// get the country if it is provided
				+ "  }" //
				+ "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" } " //
				+ "}";
			
			// store the created select query as an annotation for the current question
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
					+ createdWikiDataQuery.replace("\"", "\\\"").replace("\n", "\\n") + "\"^^xsd:string ." // the select query that should compute the answer
					+ "        ?newAnnotation qa:score \"1.0\"^^xsd:float ." // as it is rule based, a high confidence is expressed
					+ "        ?newAnnotation oa:annotatedAt ?time ." //
					+ "        ?newAnnotation oa:annotatedBy <urn:qanary:"+this.applicationName+"> ." // identify which component made this annotation
					+ "    }" //
					+ "}" //
					+ "WHERE {" //
					+ "    BIND (IRI(str(RAND())) AS ?newAnnotation) ." //
					+ "    BIND (now() as ?time) . " //
					+ "}";

			//STEP 4: Push the computed result to the Qanary triplestore
			logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
					myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
					myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
			myQanaryUtils.updateTripleStore(insertDataIntoQanaryTriplestoreQuery, myQanaryMessage.getEndpoint());

		}
		return myQanaryMessage;
	}
}
