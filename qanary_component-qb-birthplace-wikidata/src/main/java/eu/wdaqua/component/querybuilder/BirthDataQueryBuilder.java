package eu.wdaqua.component.querybuilder;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import io.swagger.v3.oas.annotations.Operation;

/**
 * represents a query builder to answer questions regarding birth place and date using Wikidata
 *
 * requirements: expects a textual question to be stored in the Qanary triplestore, 
 * written in English language, as well as previously annotated named entities
 *
 * outcome: if the question structure is supported and a previous component (NED/NER) has found 
 * named entities then this compoent constructs a Wikidata query that might be used to compute
 * the answer to the question
 *
 */

@Component
public class BirthDataQueryBuilder extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BirthDataQueryBuilder.class);

	private final String applicationName;

	private final String[] supportedQuestionPatterns = {"([Ww]here and when was )(.*)( born)"};

	public BirthDataQueryBuilder(@Value("$P{spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * compare the question against regular expression(s) representing the supported format
	 *
	 * @param questionString the textual question
	 */
	@Operation(
		summary="Check if the question is supported",
		operationId="isQuestionSupported",
		description="Compare the question against regular expression(s) representing the supported format"
	)
	private boolean isQuestionSupported(String questionString) {
		for (String pattern : this.supportedQuestionPatterns) {
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(questionString);
			logger.info("checking pattern \"{}\"", pattern);
			if (m.find())
				return true;
		}
		return false;
	}

	/**
	 * Find the position of a name in the textual question.
	 *
	 * @param questionString the textual question
	 * @param pattern a regular expression (from supportedQuestionPatterns)
	 */
	@Operation(
		summary = "Find the index of the entity in the question",
		operationId = "getNamePosition",
		description = "Find the position of a name in the textual question." //
					+ "The name is represented as a matched group within supportedQuestionPatterns."
	)
	private int getNamePosition(String questionString, String pattern) {
		Matcher m = Pattern.compile(pattern).matcher(questionString);
		m.find();
		int index = m.start(2);
		return index;
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 *
	 * @param myQanaryMessage 
	 * @throws Exception
	 */
	@Operation(
		summary = "Process a Qanary question with BirthDataQueryBuilder", //
		operationId = "process", //
		description = "Encapsulates the main functionality of this component. " //
					+ "Construct a Wikidata query to find birth date and place for named entities."
	)
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


		// This component is only supposed to answer a specific type of question.
		// Therefore we only need to continue if the question asks for birth place and date.
		// For this example is is enough to match the question against a simple regular expression.
		// However, a more sophisticated approach is possible.

		if (!this.isQuestionSupported(myQuestion)) {
			// don't continue the process if the question is not supported
			logger.info("nothing to do here as question \"{}\" does not have the supported format", 
					myQuestion);
			return myQanaryMessage;
		}

		// STEP 2: Get Wikidata entities that were annotated by OpenTapioca NED.
		//
		// In this example we are only interested in Entities that were found at a specifi point
		// in the question: 'when and where was <name> born?'.
		// Because we do not require entities that might have been found anywhere else in the 
		// question we can filter our results:

		int filterStart = this.getNamePosition(myQuestion, this.supportedQuestionPatterns[0]);
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
				+ "  ?person wdt:P19 ?specificBirthPlace . " // 
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
