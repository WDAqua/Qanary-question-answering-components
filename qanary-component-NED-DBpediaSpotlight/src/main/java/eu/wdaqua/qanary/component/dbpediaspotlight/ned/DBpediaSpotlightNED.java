package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;

/**
 * represents a wrapper of the DBpedia Spotlight service used as NED annotator
 * <p>
 * requirements: this Qanary service expects as input a textual question (that
 * is stored in the Qanary triplestore) written using English language
 * <p>
 * outcome: if DBpedia Spotlight has recognized named entities and was enabled
 * to link them to DBpedia, then this information is added to the Qanary
 * triplestore to be used by following services of this question answering
 * process
 *
 * @author Kuldeep Singh, Dennis Diefenbach, Andreas Both
 */

@Component
public class DBpediaSpotlightNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNED.class);

	@Autowired
	CacheOfRestTemplateResponse myCacheOfResponses;
	@Autowired
	RestTemplate restTemplate;

	@Inject
	private QanaryComponentConfiguration myQanaryComponentConfiguration;

	@Inject
	private DBpediaSpotlightConfiguration myDBpediaSpotlightConfiguration;

	@Inject
	private DBpediaSpotlightServiceFetcher myDBpediaSpotlightServiceFetcher;

	private final String applicationName;
	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	public DBpediaSpotlightNED(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		// STEP 1: Retrieve the information needed for the computations
		// i.e., retrieve the current question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("process question \"{}\" with DBpedia Spotlight at '{}' and minimum confidence: {}", //
				myQuestion, myDBpediaSpotlightConfiguration.getEndpoint(),
				myDBpediaSpotlightConfiguration.getConfidenceMinimum());

		// STEP2: Call the DBpedia NED service
		JsonArray resources = myDBpediaSpotlightServiceFetcher.getJsonFromService(myQuestion, //
				myDBpediaSpotlightConfiguration.getEndpoint(), //
				myDBpediaSpotlightConfiguration.getConfidenceMinimum(), //
				restTemplate, //
				myCacheOfResponses //
		);

		// get all found DBpedia resources
		List<FoundDBpediaResource> foundDBpediaResources = myDBpediaSpotlightServiceFetcher
				.getListOfResources(resources);

		// STEP3: Push the result of the component to the triplestore

		// TODO: prevent that duplicate entries are created within the
		// triplestore, here the same data is added as already exit (see
		// previous SELECT query)

		// TODO: create one larger SPARQL INSERT query that adds all discovered named
		// entities at once

		for (FoundDBpediaResource found : foundDBpediaResources) {

			QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
			bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
			bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
			bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(found.getBegin()), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(found.getEnd()), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("answer", ResourceFactory.createResource(found.getResource().toString()));
			bindingsForInsert.add("score", ResourceFactory.createTypedLiteral(String.valueOf(found.getSimilarityScore()), XSDDatatype.XSDdecimal));
			bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

			// get the template of the INSERT query
			String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
			logger.info("SPARQL query: {}", sparql);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
		}

		return myQanaryMessage;
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}

}
