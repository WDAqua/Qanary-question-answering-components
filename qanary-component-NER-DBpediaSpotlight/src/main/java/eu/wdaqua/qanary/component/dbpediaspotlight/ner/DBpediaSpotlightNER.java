package eu.wdaqua.qanary.component.dbpediaspotlight.ner;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * represents a wrapper of the DBpedia Spotlight tool used here as a spotter
 */

@Component
public class DBpediaSpotlightNER extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNER.class);

    private final String applicationName;
    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";
    private final DBpediaSpotlightConfiguration dBpediaSpotlightConfiguration;
    private final DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher;

    public DBpediaSpotlightNER( //
                                @Value("${spring.application.name}") final String applicationName, //
                                DBpediaSpotlightConfiguration dBpediaSpotlightConfiguration, //
                                DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher //
    ) {
        this.applicationName = applicationName;
        this.dBpediaSpotlightConfiguration = dBpediaSpotlightConfiguration;
        this.dBpediaSpotlightServiceFetcher = dBpediaSpotlightServiceFetcher;
        logger.debug("endpoint: {}", this.dBpediaSpotlightConfiguration.getEndpoint());

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
    }

    /**
     * default processor of a QanaryMessage
     *
     * @throws Exception
     */
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        long startTime = System.currentTimeMillis();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("Qanary Message: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint

        // the class QanaryUtils provides some helpers for standard tasks
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        // question string is required as input for the service call
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        logger.info("process question \"{}\" with DBpedia Spotlight at {} and minimum confidence: {}", myQuestion,
                dBpediaSpotlightConfiguration.getEndpoint(), dBpediaSpotlightConfiguration.getConfidenceMinimum());

        // STEP2: Call the DBpedia service
        JsonArray resources;
        resources = dBpediaSpotlightServiceFetcher.getJsonFromService(myQuestion, //
                dBpediaSpotlightConfiguration.getEndpoint(), //
                dBpediaSpotlightConfiguration.getConfidenceMinimum() //
        );

        // get all found DBpedia entities
        List<FoundDBpediaResource> foundDBpediaResources = new LinkedList<>();
        for (int i = 0; i < resources.size(); i++) {
            foundDBpediaResources.add(new FoundDBpediaResource(resources.get(i)));
            logger.debug("found entity ({} of {}): at ({},{})", //
                    i, resources.size() - 1, //
                    foundDBpediaResources.get(i).getBegin(), //
                    foundDBpediaResources.get(i).getEnd() //
            );
        }

        // STEP3: Pass the information to the component and execute it

        // create one larger SPARQL INSERT query that adds all discovered named entities
        // at once

        for (FoundDBpediaResource found : foundDBpediaResources) {

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(found.getBegin()), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(found.getEnd()), XSDDatatype.XSDnonNegativeInteger));
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
