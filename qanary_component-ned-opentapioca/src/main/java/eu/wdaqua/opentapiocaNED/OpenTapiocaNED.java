package eu.wdaqua.opentapiocaNED;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class OpenTapiocaNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaNED.class);

	@Inject
	private OpenTapiocaConfiguration openTapiocaConfiguration;

	@Inject
	private OpenTapiocaServiceFetcher openTapiocaServiceFetcher;

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// STEP 1: get the required data from the Qanary triplestore (the global process
		// memory)
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);
		String questionText = myQanaryQuestion.getTextualRepresentation();
		logger.info("processing question \"{}\" with OpenTapioca at {}.", //
				questionText, openTapiocaConfiguration.getEndpoint());

		String sparql, sparqlbind;

		JsonArray resources;
		resources = openTapiocaServiceFetcher.getJsonFromService(//
				questionText, openTapiocaConfiguration.getEndpoint());

		// TODO: get all found Wikidata resources
		List<FoundWikidataResource> foundWikidataResources = new LinkedList<>();
		logger.info("found {} terms", resources.size());

		for (int i = 0; i < resources.size(); i++) {
			JsonObject currentTerm = resources.get(i).getAsJsonObject();
			int start = currentTerm.get("start").getAsInt();
			int end = currentTerm.get("end").getAsInt();

			JsonArray tags = currentTerm.get("tags").getAsJsonArray();
			for (int j = 0; j < tags.size(); j++) {
				JsonObject entity = tags.get(j).getAsJsonObject();
				double score = entity.get("rank").getAsDouble();
				String qid = entity.get("id").getAsString();
				URI resource = new URI("https://wikidata.org/wiki/" + qid);

				foundWikidataResources.add(new FoundWikidataResource(start, end, score, resource));
				logger.info("found resouce {} for term {} at ({},{})", //
						resource, questionText.substring(start, end), start, end);
			}
		}

		// STEP 3: store computed knowledge about the given question into the Qanary
		// triplestore (the global process memory)
		logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
				myQanaryMessage.getOutGraph(), //
				myQanaryMessage.getEndpoint());
		
		// push data to the Qanary triplestore
		sparql = "" //
			+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
			+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
			+ "INSERT {";
		sparqlbind = "";
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
				+ "  ?a" + i + " oa:hasBody <" + found.getResource() + "> ;" //
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
		myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());

		return myQanaryMessage;
	}
}
