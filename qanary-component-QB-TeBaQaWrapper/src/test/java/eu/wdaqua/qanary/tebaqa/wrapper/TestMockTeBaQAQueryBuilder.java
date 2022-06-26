package eu.wdaqua.qanary.tebaqa.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.shiro.util.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.tebaqa.wrapper.messages.TeBaQAResult;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestMockTeBaQAQueryBuilder {
	private static final Logger logger = LoggerFactory.getLogger(TestMockTeBaQAQueryBuilder.class);
	private final String applicationName = "TeBaQAQueryBuilder";
	MockRestServiceServer mockServer;
	private URI endpoint;
	@Autowired
	private Environment env;
	@Autowired
	private RestTemplateWithCaching restTemplate;
	@Autowired
	@Mock
	private RestTemplateWithCaching mockedRestTemplate;
	@Autowired
	private CacheOfRestTemplateResponse myCacheOfResponse;

	// name of query file
	private final String testQueryFilename = "/queries/test_query_of_tebeqa_api.rq";

	@Before
	public void init() throws URISyntaxException, IOException {
		this.endpoint = new URI(env.getProperty("tebaqa.endpoint.url"));

		logger.info("tebaqa.endpoint.url: {}", this.endpoint);
		Assert.notNull(this.endpoint, "tebaqa.endpoint.url cannot be empty");
		Assert.notNull(this.restTemplate, "restTemplate cannot be null");
		Assert.notNull(this.mockedRestTemplate, "mockedRestTemplate cannot be null");

		this.mockServer = MockRestServiceServer.createServer(this.mockedRestTemplate);
		this.mockServer
				.expect(requestTo(this.endpoint + "?query=How%20many%20awards%20has%20Bertrand%20Russell?&lang=en"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andRespond(withSuccess("{\"answers\":[\"6\"],\"sparql\":\"" //
						+ TeBeQaTestConfiguration.getTestQuery(testQueryFilename) //
						+ "\"}", //
						MediaType.APPLICATION_JSON));
	}

	/**
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	public void testWebServiceAwardsBertrandRussell() throws URISyntaxException, IOException {
		float threshold = 0.5f;
		String langDefault = "en";
		ArrayList<String> supportedLang = new ArrayList<String>(Arrays.asList("en"));

		TeBaQAQueryBuilder teBaQAQueryBuilder = new TeBaQAQueryBuilder(threshold, langDefault, supportedLang,
				this.endpoint, this.applicationName, this.restTemplate, myCacheOfResponse);

		String question = "How many awards has Bertrand Russell?";
		TeBaQAResult result = testWebService(teBaQAQueryBuilder, question, langDefault);
		String expectedSparql = TeBeQaTestConfiguration.getTestQuery(testQueryFilename);

		assertEquals(result.getSparql(), expectedSparql);
	}

	private TeBaQAResult testWebService(TeBaQAQueryBuilder myApp, String question, String lang)
			throws URISyntaxException, UnsupportedEncodingException {
		TeBaQAResult result = myApp.requestTeBaQAWebService(this.endpoint, question, lang);
		assertFalse(result.getSparql().isEmpty());
		return result;
	}

}
