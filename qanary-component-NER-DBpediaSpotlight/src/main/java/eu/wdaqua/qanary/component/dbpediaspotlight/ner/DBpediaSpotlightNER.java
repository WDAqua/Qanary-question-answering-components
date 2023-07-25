package eu.wdaqua.qanary.component.dbpediaspotlight.ner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
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

/**
 * represents a wrapper of the DBpedia Spotlight tool used here as a spotter
 */

@Component
public class DBpediaSpotlightNER extends QanaryComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBpediaSpotlightNER.class);


    private CacheOfRestTemplateResponse myCacheOfResponses;
    private RestTemplate myRestTemplate;

    private DBpediaSpotlightConfiguration myDBpediaSpotlightConfiguration;
    private DBpediaSpotlightServiceFetcher myDBpediaSpotlightServiceFetcher;
    private boolean ignoreSslCertificate;

    private final String applicationName;
    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public DBpediaSpotlightNER(
            @Value("${spring.application.name}") final String applicationName, //
            @Autowired DBpediaSpotlightConfiguration myDBpediaSpotlightConfiguration, //
            @Autowired DBpediaSpotlightServiceFetcher myDBpediaSpotlightServiceFetcher, //
            @Autowired RestTemplate myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses //
    ) {
        this.applicationName = applicationName;
        this.myDBpediaSpotlightConfiguration = myDBpediaSpotlightConfiguration;
        this.myDBpediaSpotlightServiceFetcher = myDBpediaSpotlightServiceFetcher;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;

        LOGGER.debug("endpoint: {}", this.myDBpediaSpotlightConfiguration.getEndpoint());

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
        LOGGER.info("Qanary Message: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint

        // the class QanaryUtils provides some helpers for standard tasks
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        // question string is required as input for the service call
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        LOGGER.info("process question \"{}\" with DBpedia Spotlight at {} and minimum confidence: {}", myQuestion,
                this.myDBpediaSpotlightConfiguration.getEndpoint(),
                this.myDBpediaSpotlightConfiguration.getConfidenceMinimum());


        // STEP2: Call the DBpedia service
        JsonArray resources;
        resources = this.myDBpediaSpotlightServiceFetcher.getJsonFromService(
                myQuestion, //
                this.myDBpediaSpotlightConfiguration.getEndpoint(), //
                this.myDBpediaSpotlightConfiguration.getConfidenceMinimum(), //
                this.myRestTemplate, //
                this.myCacheOfResponses
        );

        // get all found DBpedia entities
        List<FoundDBpediaResource> foundDBpediaResources = this.myDBpediaSpotlightServiceFetcher.getListOfResources(resources);


        // STEP3: Pass the information to the component and execute it
        this.updateTriplestore(foundDBpediaResources, myQanaryQuestion, myQanaryUtils);

        return myQanaryMessage;
    }

    public String getSparqlInsertQuery(
            FoundDBpediaResource foundDBpediaResource, //
            QanaryQuestion<String> myQanaryQuestion //
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(foundDBpediaResource.getBegin()), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(foundDBpediaResource.getEnd()), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
        LOGGER.info("SPARQL query: {}", sparql);

        return sparql;
    }

    private void updateTriplestore(
            List<FoundDBpediaResource> foundDBpediaResources, //
            QanaryQuestion<String> myQanaryQuestion, //
            QanaryUtils myQanaryUtils //
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        for (FoundDBpediaResource found : foundDBpediaResources) {
            String sparql = this.getSparqlInsertQuery(found, myQanaryQuestion);

            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
        }
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }
}
