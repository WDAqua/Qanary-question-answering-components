package eu.wdaqua.qanary.component.qanswer.qb;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerRequest;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerResult;

/**
 * test the custom endpoint of this component 
 * 
 * @author anbo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestQanaryCustomController {

	private static final Logger logger = LoggerFactory.getLogger(TestQanaryCustomController.class);

	@Autowired
	private Environment env;

	@Inject
	QAnswerQueryBuilderAndSparqlResultFetcherController customController;

	private MockMvc mockMvcCustom;

	private URI realEndpoint;

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

		mockMvcCustom = MockMvcBuilders.standaloneSetup(customController).setViewResolvers(viewResolver).build();

		realEndpoint = new URI(env.getProperty("qanswer.endpoint.url"));
		assert (realEndpoint != null) : "qanswer.endpoint.url cannot be empty";
	}

	@Test 
	public void testDemoEndpoint() throws MalformedURLException, URISyntaxException {
		String question = "What is the capital of Spain";
		String lang = "en";
		String kb = "wikidata";
		
		QAnswerResult result0 = customController.requestQAnswerWebService(this.realEndpoint, question, lang, kb );
		assertTrue("the number of fetched results should be > 0, but  was " + result0.getValues().size(), result0.getValues().size() > 0);
		assertTrue("the number of fetched results should be <= 60, but  was " + result0.getValues().size(), result0.getValues().size() <= 60);
		
		QAnswerRequest requestMessage = new QAnswerRequest(question, lang, kb);		
		
		MvcResult res;
		try {
			res = mockMvcCustom.perform( //
					post(QAnswerQueryBuilderAndSparqlResultFetcherController.DEMO) //
							.content(requestMessage.asJsonString()) //
							.contentType(MediaType.APPLICATION_JSON))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.result").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.result.endpoint").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.result.knowledgebaseId").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.result.language").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.result.question").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.result.values").exists()) //
					.andReturn();
			
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}

		
		// TODO: check the content of the response fields being equal to the previous result
		try {
			res = mockMvcCustom.perform( //
					post(QAnswerQueryBuilderAndSparqlResultFetcherController.API) //
							.content(requestMessage.asJsonString()) //
							.contentType(MediaType.APPLICATION_JSON))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.endpoint").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.knowledgebaseId").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.language").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.question").exists()) //
					.andExpect(MockMvcResultMatchers.jsonPath("$.values").exists()) //
					.andReturn();
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}

		
	}

}