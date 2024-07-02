package eu.wdaqua.qanary.component.dateofdeathrealpersons.qb;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@Component
/*
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">GitHub wiki howto</a>
 */
public class QueryBuilderDateOfDeathDBpedia extends QanaryComponent {

    /*
     * Step 1: Fetching required annotations;
     * Step 2: Compute new information;
     * Step 3: Store new information in triplestore;
     */

    /*  you might use this parameterizable files to store the queries that should be
        executed for fetching the annotations, create the SPARQL queries which will be saved or
        the final insert query  */

    // query used to fetch required information from the graphURI
    private static final String QUERY_FILE_FETCH_REQUIRED_ANNOTATIONS = "/queries/select_all_AnnotationOfInstance.rq";

    // That's the later inserted query / new computed query
    private static final String QUERY_FILE_DBPEDIA_QUERY = "/queries/dbpedia_query.rq";

    // used from the Qanary commons queries, query to create a new annotation and store the computed query
    private static final Logger logger = LoggerFactory.getLogger(QueryBuilderDateOfDeathDBpedia.class);
    private final String SUPPORTED_PREFIX = "What is the date of death of ";
    private final String applicationName;

    public QueryBuilderDateOfDeathDBpedia(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // here if the files are available and do contain content // do files exist?
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERY_FILE_FETCH_REQUIRED_ANNOTATIONS);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERY_FILE_DBPEDIA_QUERY);
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component, some helping notes w.r.t. the typical 3 steps of implementing a
     * Qanary component are included in the method (you might remove all of them)
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);

        // STEP 1: Fetching the required data from the triplestore
        QanaryUtils myQanaryUtils = this.getUtils();
        QanaryTripleStoreConnector connectorToQanaryTriplestore = myQanaryUtils.getQanaryTripleStoreConnector();
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        List<String> queries = new ArrayList<>();

        logger.info("Question is: {}", myQuestion);

        // Create SPARQL-Query to fetch Entities from triplestore
        QuerySolutionMap bindingsForSelect = getBindingsForSparqlQuery(myQanaryQuestion);
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUERY_FILE_FETCH_REQUIRED_ANNOTATIONS, bindingsForSelect);
        ResultSet resultSet = connectorToQanaryTriplestore.select(sparqlQuery);

        // represents step 2 and returns the created queries which are to be stored
        try {
            queries = fetchEntitiesAndCreateQueries(myQanaryQuestion, resultSet);
        } catch (Exception e) {
            logger.error("Exception while creating queries: ", e);
        }

        // represents step 3 and stores new information in triplestore
        updateTriplestore(queries, connectorToQanaryTriplestore);

        return myQanaryMessage;
    }

    /**
     * Represents Step 3: Updating and store the triplestore with the new computed information
     *
     * @param queriesToBeInserted Query which contains an Insert-Statement which updates the triplestore
     */
    public void updateTriplestore(List<String> queriesToBeInserted, QanaryTripleStoreConnector connectorToQanaryTriplestore) throws Exception {
        for (String query : queriesToBeInserted
        ) {
            connectorToQanaryTriplestore.update(query);
        }
    }

    /**
     * Represents Step 2
     * First extract information, secondly create the SPARQL query, third create queries which will be executed to store built SPARQL queries
     *
     * @param resultSet - contains the fetch triples
     * @return queries which will be executed to store the new computed information
     */
    public List<String> fetchEntitiesAndCreateQueries(QanaryQuestion qanaryQuestion, ResultSet resultSet) throws Exception {

        // storing the list of created queries
        List<String> queries = new ArrayList<>();

        // extract information from ResultSet and create queries
        try {
            do {
                logger.info("There is a next result (t/f): {}", resultSet.hasNext());
                QuerySolution tuple = resultSet.next();
                int start = tuple.getLiteral("start").getInt();
                int end = tuple.getLiteral("end").getInt();
                String dbpediaResource = tuple.get("dbpediaResource").toString();
                logger.warn("Found matching resource <{}> at ({},{})", dbpediaResource, start, end);

                String createdQuery = this.getDbpediaQuery(dbpediaResource);
                queries.add(createdQuery);

            } while (resultSet.hasNext());
        } catch (QueryParseException e) {
            logger.error("{}", e.getMessage());
        }

        queries = createQueries(qanaryQuestion, queries);

        return queries;
    }

    /**
     * Creates the queries which will be executed to store the new computed information
     *
     * @param queries - the SPARQL queries which has been computed in 'getDbpediaQuery()'
     * @return queries which will be executed to store the new computed information
     */
    public List<String> createQueries(QanaryQuestion qanaryQuestion, List<String> queries) throws Exception {
        List<String> createdQueries = new ArrayList<>();

        logger.info("task: compute {} queries", queries.size());
        for (int i = 0; i > queries.size(); i++) {
        	String createdInsertQuery = getInsertQuery(qanaryQuestion, queries.get(i), i);
        	logger.info("created INSERT query: {}", createdInsertQuery);
            createdQueries.add(createdInsertQuery);
        }
        return createdQueries;
    }

    /**
     * @return query which will be executed to fetch required information
     */
    public QuerySolutionMap getBindingsForSparqlQuery(QanaryQuestion myQanaryQuestion) throws Exception {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));// Set the GraphID
        querySolutionMap.add("hasSource", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        querySolutionMap.add("start", ResourceFactory.createTypedLiteral(String.valueOf(SUPPORTED_PREFIX.length()), XSDDatatype.XSDnonNegativeInteger));
        querySolutionMap.add("score", ResourceFactory.createTypedLiteral("1.0", XSDDatatype.XSDfloat));

        return querySolutionMap;
    }


    /**
     * Creates the query by replacing the 'dbpediaResource' with the actual fetched dbpediaResource from the triple in the prior step
     *
     * @param dbpediaResource Resource to be inserted
     * @return the concrete SPARQL query which will be stored to the triplestore later
     */
    public String getDbpediaQuery(String dbpediaResource) throws Exception {
        try {
            QuerySolutionMap bindingsForDbpediaQuery = new QuerySolutionMap();
            bindingsForDbpediaQuery.add("dbpediaResource", ResourceFactory.createResource(dbpediaResource));
            return QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUERY_FILE_DBPEDIA_QUERY, bindingsForDbpediaQuery);
        } catch (QueryParseException e) {
            logger.error("Exception while creating the DBpediaQuery!");
            throw new QueryParseException("Exception while creating the DBpediaQuery!", 0, 0);
        }

    }

    // binds query variables with concrete values and returns the Insert-query
    public String getInsertQuery(QanaryQuestion<String> myQanaryQuestion, String createdDbpediaQuery, int index)
            throws SparqlQueryFailed, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, IOException {

        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
        bindingsForInsert.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createTypedLiteral(createdDbpediaQuery, XSDDatatype.XSDdate));
        bindingsForInsert.add("score", ResourceFactory.createTypedLiteral("1.0", XSDDatatype.XSDfloat));
        bindingsForInsert.add("index", ResourceFactory.createTypedLiteral(Integer.toString(index), XSDDatatype.XSDint));

        return QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL(bindingsForInsert);
    }

}

