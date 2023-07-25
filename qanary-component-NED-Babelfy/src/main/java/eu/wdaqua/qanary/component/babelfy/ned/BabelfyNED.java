package eu.wdaqua.qanary.component.babelfy.ned;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties
 * (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class BabelfyNED extends QanaryComponent {
	private static final Logger LOGGER = LoggerFactory.getLogger(BabelfyNED.class);
	private final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	private final String applicationName;
	private BabelfyServiceFetcher babelfyServiceFetcher;

	public BabelfyNED(
			@Value("${spring.application.name}") final String applicationName, //
			@Autowired BabelfyServiceFetcher babelfyServiceFetcher //
	) {
		this.applicationName = applicationName;
		this.babelfyServiceFetcher = babelfyServiceFetcher;

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * 
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		LOGGER.info("process: {}", myQanaryMessage);

		//STEP 1: Retrive the information needed for the question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		// Step 2: Call the babelfy service
		// fetch data from external API
		JsonArray apiResponse = this.babelfyServiceFetcher.sendRequestToApi(myQuestion);
		ArrayList<BabelfyServiceFetcher.Link> links = this.babelfyServiceFetcher.getLinksForQuestion(apiResponse);

		LOGGER.debug("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		LOGGER.debug("apply vocabulary alignment on outgraph");
		for (BabelfyServiceFetcher.Link l : links) {
			String sparql = this.getSparqlInsertQuery(l , myQanaryQuestion);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

		}
		return myQanaryMessage;
	}

	public String getSparqlInsertQuery(BabelfyServiceFetcher.Link link, QanaryQuestion<String> myQanaryQuestion) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
		QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
		bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
		bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(link.begin), XSDDatatype.XSDnonNegativeInteger));
		bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(link.end), XSDDatatype.XSDnonNegativeInteger));
		bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(link.link));
		bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

		// get the template of the INSERT query
		String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
		LOGGER.info("SPARQL query: {}", sparql);

		return sparql;
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}
}
