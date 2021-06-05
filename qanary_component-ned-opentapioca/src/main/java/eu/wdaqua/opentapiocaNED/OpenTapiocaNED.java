package eu.wdaqua.opentapiocaNED;

import java.util.List;

import com.google.gson.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import io.swagger.v3.oas.annotations.Operation;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;


/**
 * represents a wrapper of the OpenTapioca service used as NED annotator for Wikidata
 *
 * requirements: expects a textual question to be stored in the Qanary triplestore,
 * written in English language
 *
 * outcome: if named entities are recognized by OpenTapioca this information is added
 * to the Qanary triplestore to be used by other services in the question answering process
 *
 */

@Component
public class OpenTapiocaNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaNED.class);

	private final OpenTapiocaConfiguration openTapiocaConfiguration;

	private final OpenTapiocaServiceFetcher openTapiocaServiceFetcher;

	private final String applicationName;

	public OpenTapiocaNED (
			@Value("${spring.application.name}") final String applicationName,
			OpenTapiocaConfiguration openTapiocaConfiguration,
			OpenTapiocaServiceFetcher openTapiocaServiceFetcher) {
		this.applicationName = applicationName;
		this.openTapiocaConfiguration = openTapiocaConfiguration;
		this.openTapiocaServiceFetcher = openTapiocaServiceFetcher;
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 *
	 * @param myQanaryMessage 
	 * @throws Exception
	 */
	@Operation(
		summary = "Process a Qanary question with OpenTapiocaNED", //
		operationId = "process", //
		description = "Encapsulates the main functionality of this component. "
					+ "Queries the OpenTapioca endpoint to find Wikidata entities in a given "
					+ "Question and stores the result as an annotation in the Qanary triplestore."//
	)
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// STEP 1: Get the required Data
		//
		// This example component will find Wikidata entities in a given Question. 
		// As such only the textual question is required.
		
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);
		String questionText = myQanaryQuestion.getTextualRepresentation();
		logger.info("processing question \"{}\" with OpenTapioca at {}.", //
				questionText, openTapiocaConfiguration.getEndpoint());

		// STEP 2: Compute new Information about the question.
		// 
		// Use an external endpoint to an OpenTapioca implementation
		// to identify Wikidata entities in the question.

		JsonArray resources;
		resources = openTapiocaServiceFetcher.getJsonFromService(//
				questionText, openTapiocaConfiguration.getEndpoint());

		// parse the results to extract the required information:
		// - resource uri
		// - start and end position in the question
		// - score of the result
		List<FoundWikidataResource> foundWikidataResources = openTapiocaServiceFetcher.parseOpenTapiocaResults(resources);

		// STEP 3: Push the computed knowledge about the given question to the Qanary triplestore 
		//
		// This example component does not implement any further cleaning of the results. All found 
		// entities are assumed to be relevant. Depending on the specific task of the component
		// the results could be filtered to only include specific entities.

		String sparqlInsert = this.createSparqlInsertQuery(foundWikidataResources, myQanaryQuestion);

		logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
				myQanaryMessage.getOutGraph(), //
				myQanaryMessage.getEndpoint());
		
		// update the Qanary triplestore with the created insert query
		myQanaryUtils.updateTripleStore(sparqlInsert, myQanaryMessage.getEndpoint().toString());

		return myQanaryMessage;
	}

	
	/**
	 * create a SPARQL query for storing identified Wikidata entities in the Qanary Triplestore
	 *
	 * @param foundWikidataResources a list of resources retrieved from OpenTapiocaServiceFetcher
	 * @param myQanaryQuestion the QanaryQuestion currently being processed
	 * @return sparql 
	 */
	@Operation(
		summary = "Generate a SPARQL insert query", //
		operationId = "createSparqlInsertQuery", //
		description = "Creates a SPARQL query for storing identified Wikidata entities " //
					+ "(the result of this component) in the Qanary Triplestore" 
	)
	public String createSparqlInsertQuery(List<FoundWikidataResource> foundWikidataResources, QanaryQuestion myQanaryQuestion) throws Exception {
		String sparql, sparqlbind;
		
		sparql = "" //
			+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
			+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
			+ "INSERT {";
		sparqlbind = "";

		// append to the SPARQL insert query for every identified entity
		int i = 0;
		for (FoundWikidataResource found : foundWikidataResources) {
			sparql += "" //
				+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
				+ "  ?a" + i + " a qa:AnnotationOfInstance . " //
				+ "  ?a" + i + " oa:hasTarget [ " //
				+ "     a oa:SpecificResource; " //
				+ "     oa:hasSource <" + myQanaryQuestion.getUri() + ">; " //
				+ "     oa:hasSelector [ " //
				+ "         a oa:TextPositionSelector ; " //
				+ "         oa:start \"" + found.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
				+ "         oa:end \"" + found.getEnd() + "\"^^xsd:nonNegativeInteger ; " //
				+ "    ] " //
				+ "  ] . " //
				+ "  ?a" + i + " oa:hasBody <" + found.getResource() + "> ;" // the identified entity
				+ "     oa:annotatedBy <urn:qanary:component:" + this.applicationName + "> ;" //
				+ "     oa:annotatedAt ?time ; " //
				+ "     qa:score \"" + found.getScore() + "\"^^xsd:decimal ." //
				+ "}"; // end: graph
			sparqlbind += "  BIND (IRI(str(RAND())) AS ?a" + i +") .";
			i++;
		}

		sparql += "" //
			+ "} " //end: insert
			+ "WHERE { " //
			+ sparqlbind //
			+ "  BIND (now() as ?time) " //
			+ "}";

		return sparql;
	}
}

