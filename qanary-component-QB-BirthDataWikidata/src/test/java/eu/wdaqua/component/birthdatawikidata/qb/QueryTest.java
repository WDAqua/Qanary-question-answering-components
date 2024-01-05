package eu.wdaqua.component.birthdatawikidata.qb;

import eu.wdaqua.component.qb.birthdata.wikidata.Application;
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
    void filenameAnnotationsQueryTest() throws IOException {
        QuerySolutionMap bindingsForFirstname = new QuerySolutionMap();
        bindingsForFirstname.add("graph", ResourceFactory.createResource("urn:graph"));
        bindingsForFirstname.add("value", ResourceFactory.createStringLiteral("FIRST_NAME"));

        String sparqlCheckFirstname = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_ANNOTATIONS, bindingsForFirstname
        );

        assertNotNull(sparqlCheckFirstname);
        assertFalse(sparqlCheckFirstname.isEmpty());
        assertFalse(sparqlCheckFirstname.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/getAnnotationTest.rq"), sparqlCheckFirstname);
    }

    @Test
    void filenameAnnotationsFilteredQueryTest() throws IOException {
        QuerySolutionMap bindingsForAnnotation = new QuerySolutionMap();
        bindingsForAnnotation.add("graph", ResourceFactory.createResource("urn:graph"));
        bindingsForAnnotation.add("source", ResourceFactory.createResource("urn:source"));
        bindingsForAnnotation.add("filterStart", ResourceFactory.createTypedLiteral(String.valueOf(5), XSDDatatype.XSDint));

        String sparqlGetAnnotation = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA,
                bindingsForAnnotation
        );

        assertNotNull(sparqlGetAnnotation);
        assertFalse(sparqlGetAnnotation.isEmpty());
        assertFalse(sparqlGetAnnotation.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/getAnnotationFilteredTest.rq"), sparqlGetAnnotation);
    }

    @Test
    void questionAnswerFromWikidataByPersonTest() throws IOException {
        QuerySolutionMap bindingsForWikidataResultQuery = new QuerySolutionMap();
        bindingsForWikidataResultQuery.add("person", ResourceFactory.createResource("urn:person"));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON,
                bindingsForWikidataResultQuery
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/getQuestionAnswerFromWikidataByPersonTest.rq"), sparql);
    }

    @Test
    void wikidataQueryFirstAndLastNameTest() throws IOException {
        QuerySolutionMap bindingsForWikidataResultQuery = new QuerySolutionMap();
        bindingsForWikidataResultQuery.add("firstnameValue", ResourceFactory.createLangLiteral("FIRST_NAME", "en"));
        bindingsForWikidataResultQuery.add("lastnameValue", ResourceFactory.createLangLiteral("LAST_NAME", "en"));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME,
                bindingsForWikidataResultQuery
        );

        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        assertEquals(TestConfiguration.getTestQuery("queries/getQuestionAnswerFromWikidataByFirstnameLastnameTest.rq"), sparql);
    }

}