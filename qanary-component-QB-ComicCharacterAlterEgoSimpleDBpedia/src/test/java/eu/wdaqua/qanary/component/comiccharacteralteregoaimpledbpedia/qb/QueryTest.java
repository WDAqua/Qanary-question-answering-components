package eu.wdaqua.qanary.component.comiccharacteralteregoaimpledbpedia.qb;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.datatypes.xsd.XSDDatatype;
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
    void selectAnnotationQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));
        bindings.add("targetQuestion", ResourceFactory.createResource("urn:targetQuestion"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_SELECT_ANNOTATION, bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/select_annotation_test.rq"), sparql);
    }

    @Test
    void insertAnnotationQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:graph"));
        bindings.add("targetQuestion", ResourceFactory.createResource("urn:targetQuestion"));
        bindings.add("answer", ResourceFactory.createTypedLiteral("dbpediaQuery", XSDDatatype.XSDstring));
        bindings.add("score", ResourceFactory.createTypedLiteral("1.0", XSDDatatype.XSDfloat));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:applicationName"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_INSERT_ANNOTATION,
                bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/insert_one_annotation_test.rq"), sparql);
    }

    @Test
    void dbpediaQueryTest() throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("name", ResourceFactory.createStringLiteral("some name"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_DBPEDIA_QUERY,
                bindings
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/dbpedia_query_test.rq"), sparql);
    }

}