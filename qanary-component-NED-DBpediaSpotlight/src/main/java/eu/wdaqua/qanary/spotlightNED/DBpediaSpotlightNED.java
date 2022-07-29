package eu.wdaqua.qanary.spotlightNED;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

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

    /**
     * standard method for processing a message from the central Qanary component
     */
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        // STEP 1: Retrieve the information needed for the computations
        // i.e., retrieve the current question
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        logger.info("process question \"{}\" with DBpedia Spotlight at {} and minimum confidence: {}", //
                myQuestion, myDBpediaSpotlightConfiguration.getEndpoint(), myDBpediaSpotlightConfiguration.getConfidenceMinimum());
        String sparql, sparqlbind;

        // STEP2: Call the DBpedia NED service
        JsonArray resources;
        resources = myDBpediaSpotlightServiceFetcher.getJsonFromService(myQuestion, //
                myDBpediaSpotlightConfiguration.getEndpoint(), //
                myDBpediaSpotlightConfiguration.getConfidenceMinimum(), //
                restTemplate, //
                myCacheOfResponses //
        );

        // get all found DBpedia resources
        List<FoundDBpediaResource> foundDBpediaResource = new LinkedList<>();
        for (int i = 0; i < resources.size(); i++) {
            foundDBpediaResource.add(new FoundDBpediaResource(resources.get(i)));
            logger.debug("found resource ({} of {}): {} at ({},{})", //
                    i, resources.size() - 1, //
                    foundDBpediaResource.get(i).getResource(), //
                    foundDBpediaResource.get(i).getBegin(), //
                    foundDBpediaResource.get(i).getEnd() //
            );
        }

        // STEP3: Push the result of the component to the triplestore
        // TODO: prevent that duplicate entries are created within the
        // triplestore, here the same data is added as already exit (see
        // previous SELECT query)

        // create one larger SPARQL INSERT query that adds all discovered named entities
        // at once

        //TODO SPQRQL USE TEMPLATE
        sparql = "" //
                + "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                + "INSERT { ";
        sparqlbind = "";
        String name = this.myQanaryComponentConfiguration.getApplicationName();
        int i = 0;
        for (FoundDBpediaResource found : foundDBpediaResource) {
            sparql += "" //
                    + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
                    + "  ?a" + i + " a qa:AnnotationOfInstance . " //
                    + "  ?a" + i + " oa:hasTarget [ " //
                    + "           a    oa:SpecificResource; " //
                    + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
                    + "           oa:hasSelector  [ " //
                    + "                    a oa:TextPositionSelector ; " //
                    + "                    oa:start \"" + found.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
                    + "                    oa:end  \"" + found.getEnd() + "\"^^xsd:nonNegativeInteger  " //
                    + "           ] " //
                    + "  ] . " //
                    + "  ?a" + i + " oa:hasBody <" + found.getResource() + "> ;" //
                    + "     	 oa:annotatedBy <urn:qanary:" + name + "> ; " //
                    + "	    	 oa:annotatedAt ?time ; " //
                    + "     	 qa:score \"" + found.getSimilarityScore() + "\"^^xsd:decimal . " //
                    + "	}"; // end: graph
            sparqlbind += "  BIND (IRI(str(RAND())) AS ?a" + i + ") .";
            i++;
        }

        sparql += "" //
                + "} " // end: insert
                + "WHERE { " //
                + sparqlbind //
                + "  BIND (now() as ?time) " //
                + "}";
        myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());

        return myQanaryMessage;
    }

}
