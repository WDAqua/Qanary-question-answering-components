package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.pojos.AnnotationOfInstancePojo;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class KG2KGTranslateAnnotationsOfInstance extends QanaryComponent {

    /*
    - given is a annotations with either a dbpedia resource or a wikidata resource
    - depending on what is given the other resource is returned

    - 1. Step: Fetching all Annotations of type AnnotationsOfInstance
    - 2. Step:
     */

    private static final String ANNOTATIONSOFINSTANCE_RESOURCES_QUERY = "/queries/annotationsOfInstanceResourceQuery.rq";
    private static final String DBPEDIA_TO_WIKIDATA_QUERY = "/queries/dbpediaToWikidata.rq";
    private static final String WIKIDATA_TO_DBPEDIA_QUERY = "/queries/wikidataToDbpedia.rq";
    private static final String INSERT_ANNOTATION_QUERY = "/queries/insert_one_annotation.rq";
    private static final String WIKIDATA_PREFIX = "http://www.wikidata.org";
    private static final String DBPEDIA_PREFIX = "http://dbpedia.org";
    private static final String WIKIDATA_REQUEST_URI = "";
    private static final String DBPEDIA_REQUEST_URI = "";
    private final String applicationName;
    private Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstance.class);
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;

    public KG2KGTranslateAnnotationsOfInstance(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // here if the files are available and do contain content // do files exist?
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(ANNOTATIONSOFINSTANCE_RESOURCES_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(DBPEDIA_TO_WIKIDATA_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(WIKIDATA_TO_DBPEDIA_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(INSERT_ANNOTATION_QUERY);
    }

    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
        String graphID = myQanaryMessage.getOutGraph().toASCIIString();
        QanaryTripleStoreConnector qanaryTripleStoreConnector = qanaryUtils.getQanaryTripleStoreConnector();

        // Fetching resources and save them into an arraylist
        ResultSet resultSet = fetchAnnotations(graphID, qanaryUtils);
        List<AnnotationOfInstancePojo> annotationOfInstanceObjects = new ArrayList<>();

        while (resultSet.hasNext()) {
            QuerySolution entry = resultSet.next();
            String entryResource = entry.get("resource").toString();
            double score = entry.get("score").asLiteral().getDouble();
            String targetQuestion = entry.get("targetQuestion").toString();
            int start = entry.get("start").asLiteral().getInt();
            int end = entry.get("end").asLiteral().getInt();
            AnnotationOfInstancePojo tmp = new AnnotationOfInstancePojo(entryResource, targetQuestion, start, end, score);
            annotationOfInstanceObjects.add(tmp);
            logger.info("Resource found: {}", tmp.toString());
        }

        // Compute new resources
        annotationOfInstanceObjects = getEquivalentResources(annotationOfInstanceObjects);
        logger.info("Computed new resources: {}", annotationOfInstanceObjects.toString());


        // Step 3: Insert new annotations with new computed resource
        updateTriplestore(annotationOfInstanceObjects, graphID, qanaryTripleStoreConnector);


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
    - TODO: Mapping approach applicable?
    */
    public List<AnnotationOfInstancePojo> getEquivalentResources(List<AnnotationOfInstancePojo> annotationOfInstanceObjects) throws Exception {
        for (AnnotationOfInstancePojo obj : annotationOfInstanceObjects
        ) {
            String originResource = obj.getOriginResource();
            obj.setNewResource(getEquivalentResource(originResource).toString());
        }
        return annotationOfInstanceObjects;
    }

    public RDFNode getEquivalentResource(String originResource) throws Exception {
        if (originResource.contains(DBPEDIA_PREFIX)) {
            return getEquivalentResource(DBPEDIA_TO_WIKIDATA_QUERY, originResource);
        } else if (originResource.contains(WIKIDATA_PREFIX)) {
            return getEquivalentResource(WIKIDATA_TO_DBPEDIA_QUERY, originResource);
        } else
            throw new Exception();
    }

    public RDFNode getEquivalentResource(String query, String originResource) throws Exception {

        String executableQuery = getResourceRequestQuery(query, originResource);

        try {
            RDFConnection conn = RDFConnection.connect("http://dbpedia.org/sparql");
            QueryExecution queryExecution = conn.query(executableQuery);
            ResultSet resultSet = queryExecution.execSelect();
            if (resultSet.hasNext()) {
                QuerySolution querySolution = resultSet.next();
                logger.info(querySolution.get("resource").toString());
                return querySolution.get("resource");
            } else
                throw new Exception();
        } catch (Exception e) {
            throw new Exception();
        }
    }

    public String getResourceRequestQuery(String query, String originResource) throws IOException {
        logger.info("Query: {}", query);
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("originResource", ResourceFactory.createResource(originResource));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(query, bindingsForQuery);
    }

    /*
    - STEP 3: STORE COMPUTED INFORMATION
     */

    public void updateTriplestore(List<AnnotationOfInstancePojo> annotationOfInstanceObjects, String graphID, QanaryTripleStoreConnector qanaryTripleStoreConnector) throws IOException, SparqlQueryFailed {

        logger.info("{} queries have to be created.", annotationOfInstanceObjects.size());
        for (AnnotationOfInstancePojo obj : annotationOfInstanceObjects
        ) {
            String query = createInsertQuery(obj, graphID);
            qanaryTripleStoreConnector.update(query);
        }
    }

    public String createInsertQuery(AnnotationOfInstancePojo annotationOfInstancePojo, String graphID) throws IOException {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graph", ResourceFactory.createResource(graphID));
        bindingsForQuery.add("targetQuestion", ResourceFactory.createResource(annotationOfInstancePojo.getTargetQuestion()));
        bindingsForQuery.add("start", ResourceFactory.createPlainLiteral(String.valueOf(annotationOfInstancePojo.getStart())));
        bindingsForQuery.add("end", ResourceFactory.createPlainLiteral(String.valueOf(annotationOfInstancePojo.getEnd())));
        bindingsForQuery.add("answer", ResourceFactory.createResource(annotationOfInstancePojo.getNewResource()));
        bindingsForQuery.add("score", ResourceFactory.createTypedLiteral(String.valueOf(annotationOfInstancePojo.getScore()), XSDDatatype.XSDdouble));
        bindingsForQuery.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(INSERT_ANNOTATION_QUERY, bindingsForQuery);
    }


}
