package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.pojos.AnnotationOfInstancePojo;
import eu.wdaqua.qanary.component.repositories.KG2KGTranslateAnnotationsOfInstanceRepository;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.JenaConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URISyntaxException;

@Component
public class KG2KGTranslateAnnotationsOfInstance extends QanaryComponent {

    /*
    - given is an annotations with either a DBpedia resource or a Wikidata resource
    - depending on what is given the other resource is returned
     */

    private static final String ANNOTATION_OF_INSTANCE_RESOURCES_QUERY = "/queries/select_all_AnnotationOfInstance.rq";
    private static final String DBPEDIA_TO_WIKIDATA_QUERY = "/queries/dbpediaToWikidata.rq";
    private static final String WIKIDATA_TO_DBPEDIA_QUERY = "/queries/wikidataToDbpedia.rq";
    private static final String INSERT_ANNOTATION_QUERY = "/queries/insert_one_annotation.rq";
    private static final String WIKIDATA_PREFIX = "http://www.wikidata.org";
    private static final String DBPEDIA_PREFIX = "http://dbpedia.org";
    private final String applicationName;
    private final Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstance.class);
    private final Map<Boolean, String> containsDBpediaPrefix = new HashMap<>() {{
        put(true, DBPEDIA_TO_WIKIDATA_QUERY);
        put(false, WIKIDATA_TO_DBPEDIA_QUERY);
    }};
    @Autowired
    private KG2KGTranslateAnnotationsOfInstanceRepository kg2KGTranslateAnnotationsOfInstanceRepository;

    public KG2KGTranslateAnnotationsOfInstance(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // here if the files are available and do contain content // do files exist?
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(ANNOTATION_OF_INSTANCE_RESOURCES_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(DBPEDIA_TO_WIKIDATA_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(WIKIDATA_TO_DBPEDIA_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(INSERT_ANNOTATION_QUERY);
    }

    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
        String graphID = myQanaryMessage.getOutGraph().toASCIIString();
        QanaryTripleStoreConnector qanaryTripleStoreConnector = qanaryUtils.getQanaryTripleStoreConnector();

        // Step 1: Fetching resources and save them into an arraylist
        ResultSet resultSet = fetchAnnotations(graphID, qanaryUtils);
        List<AnnotationOfInstancePojo> annotationOfInstanceObjects = createAnnotationObjets(resultSet);

        // Step 2: Compute new and equivalent resources
        annotationOfInstanceObjects = computeEquivalentResources(annotationOfInstanceObjects);
        logger.info("Computed new resources: {}", annotationOfInstanceObjects.toString());

        // Step 3: Insert new annotations with new computed resource
        updateTriplestore(annotationOfInstanceObjects, graphID, qanaryTripleStoreConnector);


        return myQanaryMessage;
    }

    /*
     * STEP 1: Fetch required data
     */

    /**
     * Fetching all annotations of type qa:AnnotationsOfInstance
     */
    public ResultSet fetchAnnotations(String graphID, final QanaryUtils qanaryUtils) throws IOException, SparqlQueryFailed, URISyntaxException {
        String requestQuery = getRequestQuery(graphID);
        return qanaryUtils.getQanaryTripleStoreConnector().select(requestQuery);
    }

    /**
     * binds values for request query and returns it
     */
    public String getRequestQuery(String graphID) throws IOException {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graph", ResourceFactory.createResource(graphID));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(ANNOTATION_OF_INSTANCE_RESOURCES_QUERY, bindingsForQuery);
    }

    public List<AnnotationOfInstancePojo> createAnnotationObjets(ResultSet resultSet) {
        List<AnnotationOfInstancePojo> annotationOfInstanceObjects = new ArrayList<>();

        while (resultSet.hasNext()) {
            QuerySolution entry = resultSet.next();
            String entryResource = entry.get("hasBody").toString();
            double score = entry.get("score").asLiteral().getDouble();
            String targetQuestion = entry.get("hasSource").toString();
            int start = entry.get("start").asLiteral().getInt();
            int end = entry.get("end").asLiteral().getInt();
            String annotationId = entry.get("annotationId").toString();
            AnnotationOfInstancePojo tmp = new AnnotationOfInstancePojo(annotationId, entryResource, targetQuestion, start, end, score);
            annotationOfInstanceObjects.add(tmp);
            logger.info("Resource found: {}", tmp);
        }

        return annotationOfInstanceObjects;
    }

    /*
     * STEP 2: Compute new resources
     */

    /**
     * @param annotationOfInstanceObjects Annotation objects with missing newResource value which is added here
     * @return List with Annotation objects containing newResource values
     */
    public List<AnnotationOfInstancePojo> computeEquivalentResources(List<AnnotationOfInstancePojo> annotationOfInstanceObjects) throws IOException {
        List<AnnotationOfInstancePojo> temp = new ArrayList<>(annotationOfInstanceObjects);
        for (AnnotationOfInstancePojo annotationObject : annotationOfInstanceObjects
        ) {
            String originResource = annotationObject.getOriginResource();
            try {
                List<RDFNode> newResources = getEquivalentResource(containsDBpediaPrefix.get(originResource.contains(DBPEDIA_PREFIX)), originResource);
                logger.info("Resource(s) found for resource: {}", annotationObject.getOriginResource());
                annotationObject.setNewResources(newResources);
            } catch (RuntimeException e) {
                // no equivalent resource found -> remove this obj since it's not necessary anymore
                logger.error("No resource found for resource: {}", annotationObject.getOriginResource());
                temp.remove(annotationObject);
            }
        }
        return temp;
    }

    /**
     * used for the API endpoint
     */
    public List<RDFNode> computeEquivalentResource(String originResource) throws IOException {
        try {
            return getEquivalentResource(containsDBpediaPrefix.get(originResource.contains(DBPEDIA_PREFIX)), originResource);
        } catch (RuntimeException e) {
            logger.error("{}", e.getMessage());
            return null;
        }
    }

    /**
     * requests the DBpedia-SPARQL endpoint to fetch an equivalent resource and returns it
     *
     * @param query          used query - depending on the originResource
     * @param originResource either Wikidata or DBpedia
     * @return RDFNode containing a "resource"-key with the new resource in it
     * @throws IOException             thrown when building request query for @param executableQuery
     * @throws JenaConnectionException thrown when connection problems occur
     */
    public List<RDFNode> getEquivalentResource(String query, String originResource) throws IOException, RuntimeException {
        try {
            String executableQuery = getResourceRequestQuery(query, originResource);
            return kg2KGTranslateAnnotationsOfInstanceRepository.fetchEquivalentResource(executableQuery);
        } catch (IOException ioException) {
            logger.error("Error while creating query for resource fetching. {}", ioException.getMessage());
            throw new IOException();
        }
    }

    /**
     * binds values for request query and returns it
     */
    public String getResourceRequestQuery(String query, String originResource) throws IOException {
        logger.info("Query: {}", query);
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("originResource", ResourceFactory.createResource(originResource));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(query, bindingsForQuery);
    }

    /*
     * STEP 3: STORE COMPUTED INFORMATION
     */

    /**
     * create the insert query for every annotation and update the triplestore
     *
     * @param annotationOfInstanceObjects Annotation objects containing origin and new resource
     */
    public void updateTriplestore(List<AnnotationOfInstancePojo> annotationOfInstanceObjects, String graphID, QanaryTripleStoreConnector qanaryTripleStoreConnector) throws IOException, SparqlQueryFailed {
        for (AnnotationOfInstancePojo obj: annotationOfInstanceObjects
        ) { // inner for-loop for originResources with more than one equivalent resource
            for (RDFNode objResource : obj.getNewResources()) {
                String query = createInsertQuery(obj, objResource.toString(), graphID);
                logger.info("Created Insert Query: {}", query);
                updateTriplestoreWithQuery(query, qanaryTripleStoreConnector);
            }
        }
    }

    public void updateTriplestoreWithQuery(String query, QanaryTripleStoreConnector qanaryTripleStoreConnector) throws SparqlQueryFailed {
        qanaryTripleStoreConnector.update(query);
    }

    /**
     * binds values for insert query and returns it
     */
    public String createInsertQuery(AnnotationOfInstancePojo annotationOfInstancePojo, String newResource, String graphID) throws IOException {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graph", ResourceFactory.createResource(graphID));
        bindingsForQuery.add("targetQuestion", ResourceFactory.createResource(annotationOfInstancePojo.getTargetQuestion()));
        bindingsForQuery.add("start", ResourceFactory.createTypedLiteral(String.valueOf(annotationOfInstancePojo.getStart()), XSDDatatype.XSDinteger));
        bindingsForQuery.add("end", ResourceFactory.createTypedLiteral(String.valueOf(annotationOfInstancePojo.getEnd()), XSDDatatype.XSDinteger));
        bindingsForQuery.add("answer", ResourceFactory.createResource(newResource));
        bindingsForQuery.add("score", ResourceFactory.createTypedLiteral(String.valueOf(annotationOfInstancePojo.getScore()), XSDDatatype.XSDdouble));
        bindingsForQuery.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(INSERT_ANNOTATION_QUERY, bindingsForQuery);
    }

}
