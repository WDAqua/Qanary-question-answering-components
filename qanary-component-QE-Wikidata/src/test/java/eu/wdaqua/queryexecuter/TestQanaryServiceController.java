package eu.wdaqua.queryexecuter;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.inGraphKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.outGraphKey;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.QanaryServiceController;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestQanaryServiceController {

	private static final Logger logger = LoggerFactory.getLogger(TestQanaryServiceController.class);

	@Inject
	QanaryServiceController controller;

	private MockMvc mockMvc;

	/**
	 * initialize local controller enabled for tests
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/jsp/view/");
		viewResolver.setSuffix(".jsp");

		mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
	}

	/**
	 * test description interface
	 *
	 * @throws Exception
	 */
	@Test
	public void testDescriptionAvailable() throws Exception {
		mockMvc.perform(get(QanaryConfiguration.description)) // fetch
				.andExpect(status().isOk()) // HTTP 200
				.andReturn(); //
	}

	@Test
	public void testGetAnswersFromWikidata() {
		String query = "" //
				+ "PREFIX wikibase: <http://wikiba.se/ontology#> " //
				+ "PREFIX wd: <http://www.wikidata.org/entity/> " //
				+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " //
				+ "PREFIX bd: <http://www.bigdata.com/rdf#> " //
				+ "PREFIX p: <http://www.wikidata.org/prop/> " //
				+ "PREFIX pq: <http://www.wikidata.org/prop/qualifier/> " //
				+ "PREFIX ps: <http://www.wikidata.org/prop/statement/> " //
				+ "select DISTINCT ?personLabel ?birthplaceLabel ?birthdateLabel " //
				+ "where { " //
				+ "	 values ?allowedPropPlace { pq:P17 } " // allow 'country' as property of birthplace
				+ "  values ?person {<http://www.wikidata.org/entity/Q567>} " //
				+ "  ?person wdt:P569 ?birthdate . " // this should produce the date of birth 
				+ "  {" //
				+ "  ?person wdt:P19 ?birthplace . " // this should produce the place of birth
				+ "  }" //
				+ "	 UNION" //
				+ "  {" //
				+ "  ?person wdt:P19 ?specificBirthplace . " // 
				+ "  ?person p:P19 _:a . " //			
				+ "  _:a ps:P19 ?specificBirthPlace . " // the above place might be too specific
				+ "  _:a ?allowedPropPlace ?birthplace . "// get the country if it is provided
				+ "  }" //
				+ "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" } " //
				+ "}";

		QueryExecuter comp = new QueryExecuter("executer-test");
		String result = comp.getAnswersFromWikidata(query);
		logger.info("found: {}", result);
//		List<String> results = comp.getAnswersFromWikidata(queries);
//		for (String result : results) {
//			logger.info("found: {}", result);
//		}
	}

	@Test
	public void testIsAnswerValid() {
		QueryExecuter qe = new QueryExecuter("executer-test");
		String answerJsonValid = "{   \"head\": {     \"vars\": [ \"resource\" , \"answer\" , \"label\" ]   } ,   \"results\": {     \"bindings\": [       {         \"resource\": { \"type\": \"uri\" , \"value\": \"http://dbpedia.org/resource/Batman\" } ,         \"answer\": { \"type\": \"literal\" , \"xml:lang\": \"en\" , \"value\": \"Bruce Wayne\" } ,         \"label\": { \"type\": \"literal\" , \"xml:lang\": \"en\" , \"value\": \"Batman\" }       }     ]   } } ";
		assertTrue(qe.isAnswerValid(answerJsonValid));

		String answerJsonInvalid = "{   \"head\": {     \"vars\": [ \"resource\" , \"answer\" , \"label\" ]   } ,   \"results\": {     \"bindings\": []   } } ";
		assertFalse(qe.isAnswerValid(answerJsonInvalid));
	}

	/**
	 * send and receive message a JSON message to
	 * QanaryConfiguration.annotatequestion, check if the values are the same
	 */
	@Test
	@Ignore //TODO this test cannot be executed as the triplestore needs to be mocked first
	public void testMessageReceiveAndSend() {

		QanaryMessage requestMessage;
		try {
			requestMessage = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));
			logger.info("Message {}" + requestMessage);
		} catch (URISyntaxException e) {
			fail(e.getMessage());
			return;
		}

		// check the response
		MvcResult res;
		try {
			res = mockMvc.perform( //
					post(QanaryConfiguration.annotatequestion) //
							.content(requestMessage.asJsonString()) //
							.contentType(MediaType.APPLICATION_JSON))
					// .andExpect(status().is2xxSuccessful()) //
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
					.andReturn();
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}

		// check the values of all messages, should be equal if dummy
		// implementation is used
		QanaryMessage resultMessage;
		try {
			resultMessage = new QanaryMessage(res.getResponse().getContentAsString());
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}

		for (Entry<URI, URI> entry : requestMessage.getValues().entrySet()) {
			URI key = entry.getKey();
			int compareResult = entry.getValue().toString().compareTo(resultMessage.getValues().get(key).toString());
			assertTrue("check result vs. request: " + key, compareResult == 0);
		}

	}

	/**
	 * test correct message format
	 */
	@Test
	public void testMessageFromJson() {
		// create message from json string
		QanaryMessage message;
		try {
			message = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));

			URI endpointKeyUrlFromMessage = message.getEndpoint();
			assertNotNull(endpointKeyUrlFromMessage);

			URI endpointKeyUrlFromHere = new URI(endpointKey);

			// TODO: more tests to ensure mechanism
			assertTrue(endpointKeyUrlFromHere.toString().compareTo(endpointKeyUrlFromMessage.toString()) == 0);

		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
