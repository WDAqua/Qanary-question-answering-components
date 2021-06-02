package ue.wdaqua.opentapiocaNED;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.opentapiocaNED.OpenTapiocaNED;
import eu.wdaqua.opentapiocaNED.FoundWikidataResource;
import eu.wdaqua.opentapiocaNED.OpenTapiocaServiceFetcher;
import eu.wdaqua.opentapiocaNED.OpenTapiocaConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.LinkedList;
import java.net.URI;

@RunWith(MockitoJUnitRunner.class)
public class OpenTapiocaNEDSparqlTest {

	@Mock
	QanaryQuestion myQanaryQuestion;

	@Mock
	OpenTapiocaConfiguration openTapiocaConfiguration;

	@Mock
	OpenTapiocaServiceFetcher openTapiocaServiceFetcher;

	@InjectMocks
	OpenTapiocaNED openTapiocaNED;

	@Test
	public void testCreateSparqlInsertQuery() throws Exception {
		// mock QanaryQuestion
		URI outGraph = new URI("urn:qanary#outGraph");
		URI questionURI = new URI("qanary-test-question-uri");
		when(myQanaryQuestion.getOutGraph()).thenReturn(outGraph);
		when(myQanaryQuestion.getUri()).thenReturn(questionURI);

		// given one identified entity
		int begin = 0;
		int end = 11;
		double score = 1.0;
		URI resource = new URI("https://www.wikidata.org/entity/Q7259");

		FoundWikidataResource foundResource = new FoundWikidataResource(begin, end, score, resource);
		List<FoundWikidataResource> resources = new LinkedList<FoundWikidataResource>();
		resources.add(foundResource);

		// when the sparql insert query is created
		String sparqlInsert = openTapiocaNED.createSparqlInsertQuery(resources, myQanaryQuestion);

		// then it contains the resource uri as body parameter, 
		assertTrue(sparqlInsert.contains("?a0 oa:hasBody <" +resource.toString() + "> ;"));
		// and start and end index
		assertTrue(sparqlInsert.contains("oa:start \"" + begin + "\"^^xsd:nonNegativeInteger ;" ));
		assertTrue(sparqlInsert.contains("oa:end \"" + end + "\"^^xsd:nonNegativeInteger ;" ));
	}
}
