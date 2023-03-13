package eu.wdaqua.component.opentapioca.ned;

import eu.wdaqua.qanary.commons.QanaryQuestion;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenTapiocaNEDSparqlTest {

    @Mock
    QanaryQuestion<String> myQanaryQuestion;

    @Mock
    OpenTapiocaConfiguration openTapiocaConfiguration;

    @Mock
    OpenTapiocaServiceFetcher openTapiocaServiceFetcher;

    @InjectMocks
    OpenTapiocaNED openTapiocaNED;

    @Disabled ("This test is disabled because I removed the function to use the querybindings solutions, like we do in all the other components.")
    @Test
    void testCreateSparqlInsertQuery() throws Exception {
//        // mock QanaryQuestion
//        URI outGraph = new URI("urn:qanary#outGraph");
//        URI questionURI = new URI("qanary-test-question-uri");
//        when(myQanaryQuestion.getOutGraph()).thenReturn(outGraph);
//        when(myQanaryQuestion.getUri()).thenReturn(questionURI);
//
//        // given one identified entity
//        int begin = 0;
//        int end = 11;
//        double score = 1.0;
//        URI resource = new URI("https://www.wikidata.org/entity/Q7259");
//
//        FoundWikidataResource foundResource = new FoundWikidataResource(begin, end, score, resource);
//        List<FoundWikidataResource> resources = new LinkedList<FoundWikidataResource>();
//        resources.add(foundResource);
//
//        // when the sparql insert query is created
//        String sparqlInsert = openTapiocaNED.createSparqlInsertQuery(resources, myQanaryQuestion);
//
//        // then it contains the resource uri as body parameter,
//        assertTrue(sparqlInsert.contains("?a0 oa:hasBody <" + resource.toString() + "> ;"));
//        // and start and end index
//        assertTrue(sparqlInsert.contains("oa:start \"" + begin + "\"^^xsd:nonNegativeInteger ;"));
//        assertTrue(sparqlInsert.contains("oa:end \"" + end + "\"^^xsd:nonNegativeInteger ;"));
    }
}
