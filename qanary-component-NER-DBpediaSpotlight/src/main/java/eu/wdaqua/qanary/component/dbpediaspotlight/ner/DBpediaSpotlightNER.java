package eu.wdaqua.qanary.component.dbpediaspotlight.ner;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * represents a wrapper of the DBpedia Spotlight tool used here as a spotter
 */


public class DBpediaSpotlightNER extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNER.class);

    private final String applicationName;
    private final DBpediaSpotlightConfiguration dBpediaSpotlightConfiguration;
    private final DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher;

    public DBpediaSpotlightNER( //
                                final String applicationName, //
                                DBpediaSpotlightConfiguration dBpediaSpotlightConfiguration, //
                                DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher //
    ) {
        this.applicationName = applicationName;
        this.dBpediaSpotlightConfiguration = dBpediaSpotlightConfiguration;
        this.dBpediaSpotlightServiceFetcher = dBpediaSpotlightServiceFetcher;
        logger.debug("endpoint: {}", this.dBpediaSpotlightConfiguration.getEndpoint());
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
        String sparql, sparqlbind;
        sparql = "" //
                + "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                + "INSERT { ";
        sparqlbind = "";
        int i = 0;

        for (FoundDBpediaResource found : foundDBpediaResources) {
            sparql += "" //
                    + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
                    + "  ?a" + i + " a qa:AnnotationOfSpotInstance . " //
                    + "  ?a" + i + " oa:hasTarget [ " //
                    + "           a    oa:SpecificResource; " //
                    + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
                    + "           oa:hasSelector  [ " //
                    + "                    a oa:TextPositionSelector ; " //
                    + "                    oa:start \"" + found.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
                    + "                    oa:end  \"" + found.getEnd() + "\"^^xsd:nonNegativeInteger  " //
                    + "           ] " //
                    + "  ] ; " //
                    + "     oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; " //
                    + "	    oa:annotatedAt ?time  " //
                    + "} "; // end: graph
            sparqlbind += "  BIND (IRI(str(RAND())) AS ?a" + i + ") .";
            i++;
        }

        sparql += "" //
                + "} " // end: insert
                + "WHERE { " //
                + sparqlbind //
                + "  BIND (now() as ?time) " //
                + "}";

        myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());

        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time {}", estimatedTime);

        return myQanaryMessage;
    }
}
