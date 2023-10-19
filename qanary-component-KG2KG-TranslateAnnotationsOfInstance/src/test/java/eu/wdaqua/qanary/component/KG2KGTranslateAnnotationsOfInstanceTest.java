package eu.wdaqua.qanary.component;


import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.pojos.AnnotationOfInstancePojo;
import eu.wdaqua.qanary.component.repositories.KG2KGTranslateAnnotationsOfInstanceRepository;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
public class KG2KGTranslateAnnotationsOfInstanceTest {

    private final String DBPEDIA_TO_WIKIDATA_QUERY = "/queries/dbpediaToWikidata.rq";
    private final String WIKIDATA_TO_DBPEDIA_QUERY = "/queries/wikidataToDbpedia.rq";
    private final Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstance.class);
    private final TestDataProvider testDataProvider = new TestDataProvider();
    private final String graphID = "someTestGraphID";
    @Autowired
    private KG2KGTranslateAnnotationsOfInstance kg2KGTranslateAnnotationsOfInstance;
    @Autowired
    private KG2KGTranslateAnnotationsOfInstanceRepository kg2KGTranslateAnnotationsOfInstanceRepository;

    @Nested
    class Step1Tests {

        RDFConnection rdfConnection;
        private ResultSet resultSet;
        private String executableQuery;

        @BeforeEach
        public void setup() {
            Dataset dataset = testDataProvider.getDataset();
            rdfConnection = RDFConnection.connect(dataset);
        }

    }

    /**
     * Unit testing for step 2 methods
     */
    @Nested
    class Step2Tests {

        private final String originResourceDbpedia = "http://dbpedia.org/resource/Leipzig";
        private final String originResourceWikidata = "http://www.wikidata.org/entity/Q2079";
        private final String[] wikidataResources = {
                "http://www.wikidata.org/entity/Q3677461",
                "http://www.wikidata.org/entity/Q113624612",
                "http://www.wikidata.org/entity/Q2079"
        };

        @BeforeEach
        public void setup() {
            Dataset dataset = testDataProvider.getDataset();
            kg2KGTranslateAnnotationsOfInstanceRepository.setRdfConnection(dataset);
        }

        // Querying on an empty dataset results in exception
        @Test
        public void getEquivalentResourceTestRuntimeException() {

            Dataset dataset = DatasetFactory.create();
            kg2KGTranslateAnnotationsOfInstanceRepository.setRdfConnection(dataset);

            assertThrows(RuntimeException.class,
                    () -> kg2KGTranslateAnnotationsOfInstance.getEquivalentResource(DBPEDIA_TO_WIKIDATA_QUERY, originResourceDbpedia));

        }

        /**
         * Querying on a test-dataset involving the RDF data of the Leipzig resource
         * wikidataResources pre-defined are compared to the computed resources
         */
        @Test
        public void getEquivalentResourceTest() throws IOException {

            List<RDFNode> rdfNodes = kg2KGTranslateAnnotationsOfInstance.getEquivalentResource(DBPEDIA_TO_WIKIDATA_QUERY, originResourceDbpedia);
            logger.info("Test getEquivalentResourceTest, computed rdf nodes: {}", rdfNodes.toString());

            assertEquals(rdfNodes.size(), 3);
            assertAll("Wikidata resources correct", () -> {
                assertEquals(wikidataResources[0], rdfNodes.get(0).toString());
                assertEquals(wikidataResources[1], rdfNodes.get(1).toString());
                assertEquals(wikidataResources[2], rdfNodes.get(2).toString());
            });
        }

        @Test
        public void getEquivalentResourceTestWikidataResource() throws IOException {

            List<RDFNode> rdfNodes = kg2KGTranslateAnnotationsOfInstance.getEquivalentResource(WIKIDATA_TO_DBPEDIA_QUERY, originResourceWikidata);
            logger.info("Test getEquivalentResourceTest, computed rdf nodes: {}", rdfNodes.toString());

            assertEquals(rdfNodes.size(), 1);
            assertEquals(originResourceDbpedia, rdfNodes.get(0).toString());
        }

        /**
         * Here, a resource without an equivalent resource should return null since it catches the thrown Runtime Exception
         */
        @Test
        public void computeEquivalentResourceTest() throws IOException {

            List<RDFNode> rdfNodes = kg2KGTranslateAnnotationsOfInstance.computeEquivalentResource("originResource");
            assertNull(rdfNodes);
        }

        @Test
        public void computeEquivalentResourcesTest() throws IOException {
            // Data without set newResource value
            List<AnnotationOfInstancePojo> annotationOfInstancePojoList = testDataProvider.getAnnotationOfInstanceMissingNewResourcePojoList();
            List<AnnotationOfInstancePojo> newList = kg2KGTranslateAnnotationsOfInstance.computeEquivalentResources(annotationOfInstancePojoList);

            logger.info("Test computeEquivalentResourcesTest, pre-defined list: {} \n computed list: {}", annotationOfInstancePojoList.toString(), newList.toString());
            // The dataset is about Leipzig and the List contains 3 annotation objects where only one annotation includes Leipzig as a originResource
            assertEquals(1, newList.size());
        }

        @Test
        public void getResourceRequestQueryTest() throws IOException {
            String resourceRequestQuery = kg2KGTranslateAnnotationsOfInstance.getResourceRequestQuery(DBPEDIA_TO_WIKIDATA_QUERY, originResourceDbpedia);
            String insertedResource = "<" + originResourceDbpedia + ">" + "owl:sameAs?resource";

            logger.info("Test getResourceRequestQueryTest, insertedResource: {}", insertedResource);

            assertTrue(StringUtils.trimAllWhitespace(resourceRequestQuery).contains(insertedResource));
        }


    }

    @Nested
    class Step3Tests {

        @Mock
        private QanaryTripleStoreConnector qanaryTripleStoreConnector;

        @BeforeEach
        public void setup() throws SparqlQueryFailed {
            Mockito.doNothing().when(qanaryTripleStoreConnector).update(any());
        }

        @Test
        public void updateTriplestoreTest() throws IOException, SparqlQueryFailed {
            List<AnnotationOfInstancePojo> list = testDataProvider.getAnnotationOfInstanceCompletePojoList();
            kg2KGTranslateAnnotationsOfInstance.updateTriplestore(list, graphID, qanaryTripleStoreConnector);

            Mockito.verify(this.qanaryTripleStoreConnector, Mockito.times(5)).update(any());
        }

    }

}
