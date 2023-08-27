package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KG2KGTranslateAnnotationsOfInstance extends QanaryComponent {

    /*
    - given is a annotations with either a dbpedia resource or a wikidata resource
    - depending on what is given the counterpart is to be returned

    - 1. Step: Fetching all Annotations of type AnnotationsOfInstance
    - 2. Step:
     */

    private Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstance.class);
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private static final String ANNOTATIONSOFINSTANCE_RESOURCES_QUERY = "queries/annotationsOfInstanceResourceQuery.rq";
    private static final String COUNTER_RESOURCE_REQUEST = "queries/counterResourceRequest.rq";
    private static final String WIKIDATA_PREFIX = "http://www.wikidata.org";
    private static final String DBPEDIA_PREFIX = "http://dbpedia.org";
    private static final String WIKIDATA_REQUEST_URI = "";
    private static final String DBPEDIA_REQUEST_URI = "";


    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
        String graphID = myQanaryMessage.getOutGraph().toASCIIString();

        // Fetching resources and save them into an arraylist
        ResultSet resultSet = fetchAnnotations(graphID, qanaryUtils);
        Map<RDFNode,String> foundResources = new HashMap<>();   // are resources distinct?
        while(resultSet.hasNext()) {
            QuerySolution entry = resultSet.next();
            RDFNode entryResource = entry.get("resource");
            logger.info("Resource found: {}", entryResource);
            foundResources.put(entryResource,null);
        }

        // Compute new resources
        foundResources.forEach((k,v) -> {
            String originEntry = k.asLiteral().getString();
            if(originEntry.contains(DBPEDIA_PREFIX)) {
                getCounterResource();
            }
            else if(originEntry.contains(WIKIDATA_PREFIX)) {
                getCounterResource();
            }
        });


        return null;
    }

    /**
     * Fetching all annotations of type qa:AnnotationsOfInstance
     */
    public ResultSet fetchAnnotations(String graphID, final QanaryUtils qanaryUtils) throws IOException, SparqlQueryFailed {
        String requestQuery = getRequestQuery(graphID);
        return qanaryUtils.getQanaryTripleStoreConnector().select(requestQuery);
    }

    public String getRequestQuery(String graphID) throws IOException {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphID", ResourceFactory.createResource(graphID));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(ANNOTATIONSOFINSTANCE_RESOURCES_QUERY, bindingsForQuery);
    }

    /**
     * Step 2: Compute new resources
     */

    /*
    - has to return the concrete Resource
    - TODO: is it possible that more than one eq. resource is returned?
    -
     */
    public void getCounterResource() {

    }

}
