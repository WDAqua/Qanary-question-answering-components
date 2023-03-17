package eu.wdaqua.component.opentapioca.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


/**
 * represents a wrapper of the OpenTapioca service used as NED annotator for Wikidata
 * <p>
 * requirements: expects a textual question to be stored in the Qanary triplestore,
 * written in English language
 * <p>
 * outcome: if named entities are recognized by OpenTapioca this information is added
 * to the Qanary triplestore to be used by other services in the question answering process
 */

@Component
public class OpenTapiocaNED extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaNED.class);

    private final OpenTapiocaConfiguration openTapiocaConfiguration;

    private final OpenTapiocaServiceFetcher openTapiocaServiceFetcher;

    private final String applicationName;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public OpenTapiocaNED(
            @Value("${spring.application.name}") final String applicationName,
            OpenTapiocaConfiguration openTapiocaConfiguration,
            OpenTapiocaServiceFetcher openTapiocaServiceFetcher) {
        this.applicationName = applicationName;
        this.openTapiocaConfiguration = openTapiocaConfiguration;
        this.openTapiocaServiceFetcher = openTapiocaServiceFetcher;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);

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
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
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
        
        for (JsonElement jsonElement : resources) {
			logger.info("found resource: {}", jsonElement);
		}

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

        for (FoundWikidataResource found : foundWikidataResources) {

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(found.getBegin()), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(found.getEnd()), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("answer", ResourceFactory.createResource(found.getResource().toString()));
            bindingsForInsert.add("score", ResourceFactory.createTypedLiteral(String.valueOf(found.getScore()), XSDDatatype.XSDdecimal));
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

