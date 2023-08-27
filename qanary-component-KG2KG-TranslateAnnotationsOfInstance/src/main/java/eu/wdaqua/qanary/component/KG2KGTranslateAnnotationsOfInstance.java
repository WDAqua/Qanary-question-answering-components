package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;

import java.io.IOException;

public class KG2KGTranslateAnnotationsOfInstance extends QanaryComponent {

    /*
    - given is a annotations with either a dbpedia resource or a wikidata resource
    - depending on what is given the counterpart is to be returned

    - 1. Step: Fetching all Annotations of type AnnotationsOfInstance
    - 2. Step:
     */

    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private static final String ANNOTATIONSOFINSTANCE_RESOURCES_QUERY = "queries/annotationsOfInstanceResourceQuery.rq";


    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);

    }

    /**
     * Fetching all annotations of type qa:AnnotationsOfInstance
     */
    public String fetchAnnotations(String graphID, final QanaryUtils qanaryUtils) throws IOException, SparqlQueryFailed {
        String requestQuery = getRequestQuery(graphID);

        ResultSet results = qanaryUtils.getQanaryTripleStoreConnector().select(requestQuery);
    }

    public String getRequestQuery(String graphID) throws IOException {
        QuerySolutionMap binsingsForQuery = new QuerySolutionMap();
        binsingsForQuery.add("graphID", ResourceFactory.createResource(graphID));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(ANNOTATIONSOFINSTANCE_RESOURCES_QUERY, binsingsForQuery);
    }
}
