package eu.wdaqua.qanary.component.dateofdeathrealpersons.qb;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class QueryBuilderDateOfDeathDBpedia extends QanaryComponent {
    // you might use this parameterizable file to store the query that should be
    // executed for fetching the annotations required for this component from the
    // Qanary triplestore

    // you might use this parameterizable file to store the query that should be
    // executed for storing the annotations computed for this component from the
    // Qanary triplestore
    private static final String QUERY_FILE_FETCH_REQUIRED_ANNOTATIONS = "/queries/fetch_required_annotations.rq";
    private static final String QUERY_FILE_DBPEDIA_QUERY = "/queries/dbpedia_query.rq";
    private static final String QUERY_FILE_STORE_COMPUTED_ANNOTATIONS = "/queries/insert_one_annotation.rq";
    private static final Logger logger = LoggerFactory.getLogger(QueryBuilderDateOfDeathDBpedia.class);
    private final String applicationName;
    private String supportedPrefix = "What is the date of death of ";

    public QueryBuilderDateOfDeathDBpedia(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // here if the files are available and do contain content // do files exist?
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERY_FILE_FETCH_REQUIRED_ANNOTATIONS);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERY_FILE_DBPEDIA_QUERY);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERY_FILE_STORE_COMPUTED_ANNOTATIONS);

    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component, some helping notes w.r.t. the typical 3 steps of implementing a
     * Qanary component are included in the method (you might remove all of them)
     *
     * @throws SparqlQueryFailed
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);

        // STEP 1: Fetching the required data from the triplestore
        QanaryUtils myQanaryUtils = this.getUtils();
        QanaryTripleStoreConnector connectorToQanaryTriplestore = myQanaryUtils.getQanaryTripleStoreConnector();
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        URI qanaryQuestion = myQanaryQuestion.getUri();

        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        List<String> queries = new ArrayList<>();

        logger.info("Question is: {}", myQuestion);

        // Create SPARQL-Query to fetch Entities from triplestore
        QuerySolutionMap bindingsForSelect = getBindingsForSparqlQuery(myQanaryQuestion);
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUERY_FILE_FETCH_REQUIRED_ANNOTATIONS, bindingsForSelect);
        ResultSet resultSet = connectorToQanaryTriplestore.select(sparqlQuery);

        queries = fetchEntitiesAndCreateQueries(myQanaryMessage, myQanaryQuestion, resultSet);

        updateTriplestore(queries, connectorToQanaryTriplestore);

        return myQanaryMessage;
    }

    public void updateTriplestore(List<String> queriesToBeInserted, QanaryTripleStoreConnector connectorToQanaryTriplestore) throws Exception {
        for (String query : queriesToBeInserted
        ) {
            connectorToQanaryTriplestore.update(query);
        }
    }

    public List<String> fetchEntitiesAndCreateQueries(QanaryMessage myQanaryMessage, QanaryQuestion qanaryQuestion, ResultSet resultSet) throws Exception {

        List<String> queries = new ArrayList<String>();

        try {
            do {
                logger.info("There is a next result (t/f): " + resultSet.hasNext());
                QuerySolution tuple = resultSet.next();
                int start = tuple.getLiteral("start").getInt();
                int end = tuple.getLiteral("end").getInt();
                String dbpediaResource = tuple.get("dbpediaResource").toString();
                logger.warn("Found matching resource <{}> at ({},{})", dbpediaResource, start, end);

                String createdQuery = this.getDbpediaQuery(dbpediaResource);
                queries.add(createdQuery);

            } while (resultSet.hasNext());
        } catch (Exception e) {
            logger.warn(String.valueOf(e));
        }

        queries = createQueries(myQanaryMessage, qanaryQuestion, queries);

        return queries;
    }

    public List<String> createQueries(QanaryMessage myQanaryMessage, QanaryQuestion qanaryQuestion, List<String> queries) throws Exception {
        List<String> createdQueries = new ArrayList<>();
        for (String entity : queries
        ) {
            createdQueries.add(getInsertQuery(myQanaryMessage, qanaryQuestion, entity));
        }
        return createdQueries;
    }

    public QuerySolutionMap getBindingsForSparqlQuery(QanaryQuestion myQanaryQuestion) throws Exception {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));// Set the GraphID
        querySolutionMap.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        querySolutionMap.add("startValue", ResourceFactory.createTypedLiteral(String.valueOf(supportedPrefix.length()), XSDDatatype.XSDnonNegativeInteger));

        return querySolutionMap;
    }


    public String getDbpediaQuery(String dbpediaResource) throws IOException {

        QuerySolutionMap bindingsForDbpediaQuery = new QuerySolutionMap();
        bindingsForDbpediaQuery.add("dbpediaResource", ResourceFactory.createResource(dbpediaResource));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUERY_FILE_DBPEDIA_QUERY, bindingsForDbpediaQuery);
    }

    public String getInsertQuery(QanaryMessage myQanaryMessage, QanaryQuestion<String> myQanaryQuestion, String createdDbpediaQuery)
            throws SparqlQueryFailed, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, IOException {

        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
        bindingsForInsert.add("body", ResourceFactory.createTypedLiteral(createdDbpediaQuery, XSDDatatype.XSDdate));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUERY_FILE_STORE_COMPUTED_ANNOTATIONS, bindingsForInsert);
    }

}

