package eu.wdaqua.qanary.component.simplequerybuilderandexecutor.qbe;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class QueryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTest.class);

    @Test
    void selectClassesQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_SELECT_CLASSES, bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/select_classes_test.rq"), sparql);
    }

    @Test
    void selectPropertiesQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_SELECT_PROPERTIES,
                bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/select_properties_test.rq"), sparql);
    }

    @Test
    void selectEntitiesQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_SELECT_ENTITIES,
                bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/select_entities_test.rq"), sparql);
    }

    @Test
    void insertSparqlQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));
        bindings.add("body", ResourceFactory.createStringLiteral("generatedQuery"));
        bindings.add("answer", ResourceFactory.createResource("urn:answer"));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:applicationName"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_INSERT_SPARQL,
                bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/insert_sparql_test.rq"), sparql);
    }

    @Test
    void insertJsonQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));
        bindings.add("body", ResourceFactory.createStringLiteral("json"));
        bindings.add("answer", ResourceFactory.createResource("urn:answer"));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:applicationName"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_INSERT_JSON,
                bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/insert_json_test.rq"), sparql);
    }

}