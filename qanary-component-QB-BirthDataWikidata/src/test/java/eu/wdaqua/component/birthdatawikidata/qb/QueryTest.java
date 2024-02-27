package eu.wdaqua.component.birthdatawikidata.qb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import eu.wdaqua.component.qb.birthdata.wikidata.Application;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTest.class);

    private PrefixMappingImpl prefixMap; 

    private ParsedQuery parseQuery(String queryString) {
        List<String> graphs = new LinkedList<>();
        List<String> filters = new LinkedList<>();
        List<TriplePath> triples = new LinkedList<>();

        Query query = QueryFactory.create(queryString);
        graphs  = query.getGraphURIs();
        ElementWalker.walk(query.getQueryPattern(), 
            new ElementVisitorBase() {
                // for visiting Filter statements
                public void visit(ElementFilter filter) {
                    filters.add(filter.getExpr().toString());
                }
                // for visiting a block of Triples
                public void visit(ElementPathBlock el) {
                    // iterate of all Triples in the block
                    Iterator<TriplePath> triplePaths = el.patternElts();
                    while (triplePaths.hasNext()) {
                        // and add them to the list of visited triples (for later assertions)
                        TriplePath triple = triplePaths.next();
                        triples.add(triple);
                    }
                } 
            }
        );

        return new ParsedQuery(graphs, filters, triples);
    }

    private class ParsedQuery {
        private List<String> graphs = new LinkedList<>();
        private List<String> filters = new LinkedList<>();
        private List<TriplePath> triples = new LinkedList<>();

        public ParsedQuery(List<String> graphs, List<String> filters, List<TriplePath> triples) {
            this.graphs = graphs;
            this.filters = filters;
            this.triples = triples;
        }

        // TODO: DOCS
        public boolean containsGraph(String expectedGraph) {
            return this.graphs.stream().anyMatch(o -> expectedGraph.equals(o.toString()));
        }
        public boolean containsFilterKeyValuePair(String expectedKey, String expectedValue) {
            return this.filters.stream().anyMatch(
                    o -> (o.toString().contains(expectedKey) && o.toString().contains(expectedValue))
            );
        }
        public boolean containsTriple(String subject, String predicate, String object, PrefixMappingImpl prefixMap) {
            return this.triples.stream().anyMatch(
                    o -> (subject.equals(o.getSubject().toString(prefixMap)) && 
                        predicate.equals(o.getPredicate().toString(prefixMap)) && 
                        object.equals(o.getObject().toString(prefixMap)))
                    // works only for URIs; 
                    // literals are matched **with their datatype or language** 
                    // and "en" would become en@ (no language assigned)
            );
        }
        public boolean containsTripleWithLiteral(String subject, String predicate, String object, PrefixMappingImpl prefixMap) {
            return this.triples.stream().anyMatch(
                    o -> (subject.equals(o.getSubject().toString(prefixMap)) && 
                        predicate.equals(o.getPredicate().toString(prefixMap)) && 
                        object.equals(o.getObject().getLiteralValue().toString()))
            );
        }
        public boolean containsTripleWithLiteralDatatype(String subject, String predicate, String object, RDFDatatype objectDatatype, PrefixMappingImpl prefixMap) {
            return this.triples.stream().anyMatch(
                    o -> (subject.equals(o.getSubject().toString(prefixMap)) && 
                        predicate.equals(o.getPredicate().toString(prefixMap)) && 
                        object.equals(o.getObject().getLiteralValue().toString()) &&
                        objectDatatype.equals(o.getObject().getLiteralDatatype()))
            );
        }
        public boolean containsTripleWithLiteralLanguage(String subject, String predicate, String object, String objectLanguage, PrefixMappingImpl prefixMap) {
            return this.triples.stream().anyMatch(
                    o -> (subject.equals(o.getSubject().toString(prefixMap)) && 
                        predicate.equals(o.getPredicate().toString(prefixMap)) && 
                        object.equals(o.getObject().getLiteralValue().toString()) &&
                        objectLanguage.equals(o.getObject().getLiteralLanguage()))
            );
        }
        // TODO: consider methods for complete list matches
        //public boolean containsAllGraphs(List<String> expectedGraphs) {
        //    return CollectionUtils.isEqualCollection(this.graphs, expectedGraphs);
        //}
        // public boolean containsAllFilterKeyValuePairs() {}
        // public boolean containsAllTriples() {}

        public List<String> getGraphs() {
            return graphs;
        }

        public List<String> getFilters() {
            return filters;
        }

        public List<TriplePath> getTriples() {
            return triples;
        }

    }

    @BeforeAll
    public void initPrefixmap() {
        // define prefix mappings for comparing URIs
        this.prefixMap = new PrefixMappingImpl();
        this.prefixMap.setNsPrefix("oa", "http://www.w3.org/ns/openannotation/core/");
        this.prefixMap.setNsPrefix("wdt", "http://www.wikidata.org/prop/direct/");
        this.prefixMap.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        this.prefixMap.setNsPrefix("bd", "http://www.bigdata.com/rdf#");
        this.prefixMap.setNsPrefix("wikibase", "http://wikiba.se/ontology#");
    }

    @Test
    void filenameAnnotationsQueryTest() throws IOException {
        String expectedGraph = "urn:graph";
        String expectedValue = "FIRST_NAME";

        QuerySolutionMap bindingsForFirstname = new QuerySolutionMap();
        bindingsForFirstname.add("graph", ResourceFactory.createResource(expectedGraph));
        bindingsForFirstname.add("value", ResourceFactory.createStringLiteral(expectedValue));

        String sparqlCheckFirstname = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_ANNOTATIONS, bindingsForFirstname
        );
        assertNotNull(sparqlCheckFirstname);
        assertFalse(sparqlCheckFirstname.isEmpty());
        assertFalse(sparqlCheckFirstname.isBlank());
        
        // init query
        // TODO: change to parseQuery()
        ParsedQuery parsedQuery = parseQuery(sparqlCheckFirstname);
        // check graph
        assert parsedQuery.containsGraph(expectedGraph);
        assert parsedQuery.containsFilterKeyValuePair("?wikidataResource", expectedValue);
        // TODO: only for POC; this is not actually required
        assert parsedQuery.containsTriple("?target", "oa:hasSelector", "?textSelector", prefixMap);
    }

    @Test
    @Disabled
    void filenameAnnotationsFilteredQueryTest() throws IOException {
        String expectedGraph = "urn:graph";
        String expectedSource = "urn:source";
        String expectedStart = String.valueOf(5);

        QuerySolutionMap bindingsForAnnotation = new QuerySolutionMap();
        bindingsForAnnotation.add("graph", ResourceFactory.createResource(expectedGraph));
        bindingsForAnnotation.add("hasSource", ResourceFactory.createResource(expectedSource));
        bindingsForAnnotation.add("start", ResourceFactory.createTypedLiteral(expectedStart, XSDDatatype.XSDint));

        String sparqlGetAnnotation = QanaryTripleStoreConnector.readFileFromResourcesWithMap(

                TestConfiguration.FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA,
                bindingsForAnnotation
        );
        assertNotNull(sparqlGetAnnotation);
        assertFalse(sparqlGetAnnotation.isEmpty());
        assertFalse(sparqlGetAnnotation.isBlank());

        ParsedQuery parsedQuery = parseQuery(sparqlGetAnnotation);
        assert parsedQuery.containsGraph(expectedGraph);
        // TODO: test fails, check filter statement
        assert parsedQuery.containsFilterKeyValuePair("?start", expectedStart);
        assert parsedQuery.containsTriple("?target", "oa:hasSource", expectedSource, prefixMap);
    }

    @Test
    void questionAnswerFromWikidataByPersonTest() throws IOException {
        String expectedPerson = "urn:person";

        QuerySolutionMap bindingsForWikidataResultQuery = new QuerySolutionMap();
        bindingsForWikidataResultQuery.add("person", ResourceFactory.createResource(expectedPerson));

        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON,
                bindingsForWikidataResultQuery
        );
        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        ParsedQuery parsedQuery = parseQuery(sparql);
        assert parsedQuery.containsTriple(expectedPerson, "wdt:P735", "?firstname", prefixMap);
        assert parsedQuery.containsTriple(expectedPerson, "wdt:P569", "?birthdate", prefixMap);
        assert parsedQuery.containsTriple(expectedPerson, "wdt:P19", "?birthplace", prefixMap);
        assert parsedQuery.containsTriple(expectedPerson, "wdt:P19", "?specificBirthPlace", prefixMap);

        // exact comparison: 
        // assertEquals(TestConfiguration.getTestQuery("queries/getQuestionAnswerFromWikidataByPersonTest.rq").concat("\n"), sparql);
    }

    @Test
    void wikidataQueryFirstAndLastNameTest() throws IOException {
        String expectedFirstName = "FIRST_NAME";
        String expectedLastName = "LAST_NAME";
        String expectedLanguage = "en";

        QuerySolutionMap bindingsForWikidataResultQuery = new QuerySolutionMap();
        bindingsForWikidataResultQuery.add("firstnameValue", ResourceFactory.createLangLiteral(expectedFirstName, expectedLanguage));
        bindingsForWikidataResultQuery.add("lastnameValue", ResourceFactory.createLangLiteral(expectedLastName, expectedLanguage));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                TestConfiguration.FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME,
                bindingsForWikidataResultQuery
        );
        assertNotNull(sparql);
        assertFalse(sparql.isEmpty());
        assertFalse(sparql.isBlank());

        ParsedQuery parsedQuery = parseQuery(sparql);
        assert parsedQuery.containsTripleWithLiteralLanguage("?firstname", "rdfs:label", expectedFirstName, expectedLanguage, prefixMap);
        assert parsedQuery.containsTripleWithLiteralLanguage("?lastname", "rdfs:label", expectedLastName, expectedLanguage, prefixMap);
        assert parsedQuery.containsTripleWithLiteral("bd:serviceParam", "wikibase:language", expectedLanguage, prefixMap);

        // exact comparison: 
        // assertEquals(TestConfiguration.getTestQuery("queries/getQuestionAnswerFromWikidataByFirstnameLastnameTest.rq").concat("\n"), sparql);
    }

}
