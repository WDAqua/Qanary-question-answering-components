package eu.wdaqua.opentapiocaNED;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import io.swagger.v3.oas.annotations.Operation;

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
public class OpenTapiocaNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaNED.class);

	private final OpenTapiocaConfiguration openTapiocaConfiguration;

	private final OpenTapiocaServiceFetcher openTapiocaServiceFetcher;

	public OpenTapiocaNED (OpenTapiocaConfiguration openTapiocaConfiguration, OpenTapiocaServiceFetcher openTapiocaServiceFetcher) {
		this.openTapiocaConfiguration = openTapiocaConfiguration;
		this.openTapiocaServiceFetcher = openTapiocaServiceFetcher;
	}

	@Override
	@Operation(
		summary = "Process a Qanary question with OpenTapiocaNED", //
		operationId = "process", //
		description = "Encapsulates the main functionality of this component. "
		+ "Queries the OpenTapioca endpoint to find Wikidata entities in a given Question "
		+ "and stores the result as an annotation in the Qanary triplestore."//
	)
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
		// At this point the external endpoint to an OpenTapioca implementation is used
		// to identify Wikidata entities in the question.
		JsonArray resources;
		resources = openTapiocaServiceFetcher.getJsonFromService(//
				questionText, openTapiocaConfiguration.getEndpoint());

		// parse the results to extract the required information:
		// - resource uri
		// - start and end position in the question
		// - score (rank) of the result
		List<FoundWikidataResource> foundWikidataResources = openTapiocaServiceFetcher.parseOpenTapiocaResults(resources);

		// STEP 3: Push the computed knowledge about the given question to the Qanary triplestore 
		// TODO: refactor this step
		// This example component does not require any further cleaning of the results. All found 
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

	@Operation(
		summary = "Create a SPARQL query for storing identified Wikidata entities in the Triplestore", //
		operationId = "createSparqlInsertQuery", //
		description = "" // TODO: add description
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
				+ "     oa:annotatedBy <" + openTapiocaConfiguration.getEndpoint() + "> ;" //
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

