package eu.wdaqua.qanary.component;


import eu.wdaqua.qanary.component.repositories.KG2KGTranslateAnnotationsOfInstanceRepository;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
public class KG2KGTranslateAnnotationsOfInstanceTest {

    private final String DBPEDIA_TO_WIKIDATA_QUERY = "/queries/dbpediaToWikidata.rq";
    private final String WIKIDATA_TO_DBPEDIA_QUERY = "/queries/wikidataToDbpedia.rq";
    private final Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstance.class);
    @Autowired
    private KG2KGTranslateAnnotationsOfInstance kg2KGTranslateAnnotationsOfInstance;

    @Nested
    class Step1Tests {

        @Test
        public void getRequestQueryTest() throws IOException {

            String testGraphID = "someTestGraphID";

            String requestQuery = kg2KGTranslateAnnotationsOfInstance.getRequestQuery(testGraphID);
            //   assertTrue(requestQuery.contains("FROM <" + testGraphID + ">"));
        }

        @Test
        public void createAnnotationObjectsTest() {

        }
    }

    @Nested
    class Step2Tests {

        private final String originResource = "http://dbpedia.org/resource/Leipzig";
        @Autowired
        private KG2KGTranslateAnnotationsOfInstanceRepository kg2KGTranslateAnnotationsOfInstanceRepository;
        private String query;
        private TestDataProvider testDataProvider;

        @BeforeEach
        public void setup() throws IOException {
            //  query = kg2KGTranslateAnnotationsOfInstance.getResourceRequestQuery(DBPEDIA_TO_WIKIDATA_QUERY, originResource);
            testDataProvider = new TestDataProvider();
        }

        @Test
        public void getEquivalentResourceTestRuntimeException() throws IOException {

            Dataset dataset = DatasetFactory.create();
            kg2KGTranslateAnnotationsOfInstanceRepository.setRdfConnection(dataset);

            assertThrows(RuntimeException.class,
                    () -> kg2KGTranslateAnnotationsOfInstance.getEquivalentResource(DBPEDIA_TO_WIKIDATA_QUERY, originResource));

        }

        @Test
        public void getEquivalentResourceTest() throws IOException {

            Dataset dataset = testDataProvider.getDataset();
            kg2KGTranslateAnnotationsOfInstanceRepository.setRdfConnection(dataset);
            String[] wikidataResources = {
                    "http://www.wikidata.org/entity/Q2079",
                    "http://www.wikidata.org/entity/Q3677461",
                    "http://www.wikidata.org/entity/Q113624612"
            };

            //  RDFNode rdfNode = kg2KGTranslateAnnotationsOfInstance.getEquivalentResource(DBPEDIA_TO_WIKIDATA_QUERY, originResource);
        }

        @Test
        public void computeEquivalentResourcesTest() throws IOException {

        }

        @Test
        public void getResourceRequestQueryTest() {
            assertTrue(query.contains("<" + originResource + ">"));
        }


    }

    @Nested
    class Step3Test {

        @Test
        public void createInsertQueryTest() {

        }

    }

}
