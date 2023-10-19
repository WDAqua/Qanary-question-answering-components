package eu.wdaqua.qanary.component.mypackage;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.Application;
import eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.QueryBuilderDateOfDeathDBpedia;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class QanaryServiceQueryBuilderDateOfDeathDBpediaControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(QanaryServiceQueryBuilderDateOfDeathDBpediaControllerTest.class);
    private static final String QUERY_FILE_STORE_COMPUTED_ANNOTATIONS = "/queries/insert_one_AnnotationOfAnswerSPARQL.rq";
    private final static String SUPPORTED_PREFIX = "What is the date of death of ";
    private final static String EXAMPLE_QUESTION = "exampleQuestion";
    private final static String ENDPOINT = "endpoint";
    private final static String IN_GRAPH = "inGraph";
    private final static String OUT_GRAPH = "outGraph";

    QanaryMessage qanaryMessage;
    QanaryQuestion qanaryQuestion;
    ResultSet resultSet;
    @Autowired
    QueryBuilderDateOfDeathDBpedia queryBuilderDateOfDeathDBpedia;
    @MockBean
    QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext applicationContext;

    public QanaryServiceQueryBuilderDateOfDeathDBpediaControllerTest() {
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERY_FILE_STORE_COMPUTED_ANNOTATIONS);
    }

    /**
     * initialize local controller enabled for tests
     */
    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    /**
     * Implementing function which creates a resultSet for testing purposes
     */
    ResultSet createResultSet(List<QuerySolutionMap> querySolutionMapList) {

        return new ResultSet() {
            final List<QuerySolutionMap> querySolutionMaps = querySolutionMapList;
            int rowIndex = -1;

            @Override
            public boolean hasNext() {
                return rowIndex < querySolutionMapList.size() - 1;
            }

            @Override
            public QuerySolution next() {
                rowIndex++;
                return querySolutionMaps.get(rowIndex);
            }

            @Override
            public void forEachRemaining(Consumer<? super QuerySolution> consumer) {

            }

            @Override
            public QuerySolution nextSolution() {
                return null;
            }

            @Override
            public Binding nextBinding() {
                return null;
            }

            @Override
            public int getRowNumber() {
                return 0;
            }

            @Override
            public List<String> getResultVars() {
                return null;
            }

            @Override
            public Model getResourceModel() {
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove() is not supported.");
            }

            @Override
            public void close() {
            }
        };
    }

    /**
     * @return
     */
    List<QuerySolutionMap> addQuerySolutionMapForQuestion() {
        Model model = ModelFactory.createDefaultModel();
        List<QuerySolutionMap> querySolutionMapList = new ArrayList<>();
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("question", model.createResource(QanaryServiceQueryBuilderDateOfDeathDBpediaControllerTest.EXAMPLE_QUESTION));
        querySolutionMapList.add(querySolutionMap);
        return querySolutionMapList;
    }

    /**
     * Creates a ResultSet for Structure of TupleTestObject-Class for some tests below
     *
     * @param tupleTestObjects List of Objects to create QuerySolutionMaps for
     * @return List of QuerySolutionMap(s) to pass to another method for creating a ResultSet
     */
    List<QuerySolutionMap> addQuerySolutionMapForTupleTestObject(List<TupleTestObject> tupleTestObjects) {
        Model model = ModelFactory.createDefaultModel();
        List<QuerySolutionMap> querySolutionMapList = new ArrayList<>();
        for (TupleTestObject tupleTestObject : tupleTestObjects
        ) {
            QuerySolutionMap querySolutionMap = new QuerySolutionMap();
            querySolutionMap.add("start", ResourceFactory.createTypedLiteral(tupleTestObject.getStart()));
            querySolutionMap.add("end", model.createTypedLiteral(tupleTestObject.getEnd()));
            querySolutionMap.add("dbpediaResource", model.createResource(tupleTestObject.getdbpediaResource()));
            querySolutionMapList.add(querySolutionMap);
        }
        return querySolutionMapList;
    }

    @Test
    public void testDecodingOfGetDbpediaQueryRequest() throws Exception {
        String encodedURI = "http%3A%2F%2Fdbpedia.org%2Fresource%2FStephen_Hawking";
        String correctURI = "http://dbpedia.org/resource/Stephen_Hawking";
        String expectedQuery = queryBuilderDateOfDeathDBpedia.getDbpediaQuery(correctURI);

        MvcResult mvcResult = mockMvc.perform(get("/getdbpediaquery/" + encodedURI))
                .andExpect(status().isOk())
                .andReturn();


        assertEquals(expectedQuery, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Testing the "createQueriesWithGivenEntities()"-Method
     */
    @Nested
    class CreateQueriesTests {

        /**
         * Testing the createQueries-Method
         * INPUT: DBpedia-Queries or in this case entities which represent the created DBpedia-Queries
         * RETURN: Insert-Queries with entities (real: dbpediaQueries) inserted
         */
        @BeforeEach
        public void setup() throws URISyntaxException, SparqlQueryFailed {
            // create ResultSet for ExampleQuestion
            ResultSet resultSetQuestion = createResultSet(addQuerySolutionMapForQuestion());
            Mockito.when(qanaryTripleStoreConnector.select(any())).thenReturn(resultSetQuestion);

            qanaryMessage = new QanaryMessage(new URI(ENDPOINT), new URI(IN_GRAPH), new URI(OUT_GRAPH));
            qanaryQuestion = new QanaryQuestion<>(qanaryMessage, qanaryTripleStoreConnector);
        }

        @Test
        public void createQueriesWithGivenEntities() throws Exception {
            List<String> givenEntities = List.of("testCase1", "testCase2", "TESTcASE3");

            List<String> result = queryBuilderDateOfDeathDBpedia.createQueries(qanaryQuestion, givenEntities);

            assertEquals(3, result.size());
            assertAll("Result Queries contain given Entities",
                    () -> {
                        for (int i = 0; i < result.size(); i++) {
                            assertTrue(result.get(i).contains(givenEntities.get(i)));
                            String queryTemplate = QUERY_FILE_STORE_COMPUTED_ANNOTATIONS;
                        }
                    }
            );
        }
    }

    /**
     * Testing the "fetchEntitiesAndCreateQueries"-Method
     * INPUT: ResultSet w/ start::Int, end::Int, dbpediaResource::String
     * RETURN: DBpedia-Queries
     * getDbpediaQuery gets called => Problem with Mock or not Mock of QueryBuilderDateOfDeath-Class
     */
    @Nested
    class fetchEntitiesAndCreateQueriesTests {

        List<TupleTestObject> tupleTestObjects;

        @BeforeEach
        void setup() throws SparqlQueryFailed, URISyntaxException {

            // test data
            tupleTestObjects = new ArrayList<>();
            tupleTestObjects.add(new TupleTestObject(1, 2, "https://dbpedia.org/page/Stephen_Hawking"));
            tupleTestObjects.add(new TupleTestObject(5, 2, "https://dbpedia.org/page/Albert_Einstein"));
            tupleTestObjects.add(new TupleTestObject(89, 221, "https://dbpedia.org/page/Edsger_Dijkstra"));

            // set up resultSet for example-question for qanarytriplestore
            ResultSet resultSetQuestion = createResultSet(addQuerySolutionMapForQuestion());
            Mockito.when(qanaryTripleStoreConnector.select(any())).thenReturn(resultSetQuestion);

            // set up qanaryMessage abd qanaryQuestion
            qanaryMessage = new QanaryMessage(new URI(ENDPOINT), new URI(IN_GRAPH), new URI(OUT_GRAPH));
            qanaryQuestion = new QanaryQuestion<>(qanaryMessage, qanaryTripleStoreConnector);
        }

        @Test
        void fetchEntitiesAndCreateQueriesWithGivenResultSet() throws Exception {
            List<QuerySolutionMap> querySolutionMaps = addQuerySolutionMapForTupleTestObject(tupleTestObjects);
            resultSet = createResultSet(querySolutionMaps);

            List<String> queries = queryBuilderDateOfDeathDBpedia.fetchEntitiesAndCreateQueries(qanaryQuestion, resultSet);

            // Test for queries
            assertAll("Queries contain values from TupleObjects",
                    () -> assertEquals(3, queries.size()),
                    () -> {
                        for (int i = 0; i < queries.size(); i++) {
                            assertTrue(queries.get(i).contains(tupleTestObjects.get(i).getdbpediaResource()));
                        }
                    }
            );

        }
    }

    /**
     * Testing the "getBindingsForSparqlQuery"-Method
     */
    @Nested
    class getBindingForSparqlQueryTests {

        @BeforeEach()
        void setup() throws URISyntaxException, SparqlQueryFailed {
            resultSet = createResultSet(addQuerySolutionMapForQuestion());
            Mockito.when(qanaryTripleStoreConnector.select(any())).thenReturn(resultSet);
            qanaryMessage = new QanaryMessage(new URI(ENDPOINT), new URI(IN_GRAPH), new URI(OUT_GRAPH));
            qanaryQuestion = new QanaryQuestion<>(qanaryMessage, qanaryTripleStoreConnector);
        }

        @Test
        void getBindingForSparqlQueryWithGivenQanaryQuestion() throws Exception {
            QuerySolutionMap querySolutionMap = (queryBuilderDateOfDeathDBpedia.getBindingsForSparqlQuery(qanaryQuestion));
            Map<String, RDFNode> solutionsAsMap = querySolutionMap.asMap();
            assertAll("Check correct Map",
                    () -> assertEquals(solutionsAsMap.get("graph").toString(), qanaryMessage.getOutGraph().toString()),
                    () -> assertEquals(solutionsAsMap.get("targetQuestion").toString(), EXAMPLE_QUESTION),
                    () -> assertEquals(solutionsAsMap.get("startValue").asLiteral(),
                            ResourceFactory.createTypedLiteral(String.valueOf(SUPPORTED_PREFIX.length()), XSDDatatype.XSDnonNegativeInteger))
            );
        }
    }

}
