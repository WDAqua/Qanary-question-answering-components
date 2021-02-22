package eu.wdaqua.qanary.spotlightNED;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;

import com.google.gson.JsonArray;

import eu.wdaqua.qanary.commons.config.CacheConfig;
import eu.wdaqua.qanary.commons.config.RestClientConfig;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class, classes = { RestClientConfig.class,
		CacheConfig.class, Application.class })
class DBpediaSpotlightServiceFetcherTest {

	// time span for caching, tests wait this time span during the test runs
	private final static int MAX_TIME_SPAN_SECONDS = 5;

	// define here the current CaffeineCacheManager configuration
	final static float dbpediaspotlightConfidenceMinimum = 0.5f;
	final static String dbpediaSpotlightEndpoint = "https://api.dbpedia-spotlight.org/en/annotate";
	static {
		System.setProperty("qanary.webservicecalls.cache.specs",
				"maximumSize=1000,expireAfterAccess=" + MAX_TIME_SPAN_SECONDS + "s");
		System.setProperty("server.port", "44444");
		System.setProperty("spring.application.name", "DBpediaSpotlightServiceFetcherTest");
		System.setProperty("spring.boot.admin.url", "http://127.0.0.1");
		System.setProperty("spring.boot.admin.client.url", "http://127.0.0.1");
		System.setProperty("dbpediaspotlight.confidence.minimum", "" + dbpediaspotlightConfidenceMinimum);
	}

	@Autowired
	DBpediaSpotlightServiceFetcher myFetcher;

	@Autowired
	RestTemplateWithCaching myRestTemplate;

	@Autowired
	CacheOfRestTemplateResponse myCacheResponse;

	/**
	 * run several web service requests to DBpedia spotlight where most should be cached
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws InterruptedException
	 */
	@Test
	void testGetJsonFromService() throws UnsupportedEncodingException, InterruptedException {

		assertNotNull(myFetcher);
		assertNotNull(myRestTemplate);

		String[] questions = { "Where is London?", "Who is Angela Merkel?", "What is the Eiffel Tower?" };

		for (int i = 0; i < questions.length; i++) {
			annotateQuestion(questions[i]);
		}

		TimeUnit.SECONDS.sleep(MAX_TIME_SPAN_SECONDS + 1); // wait until it is too late for caching

		for (int i = 0; i < questions.length; i++) {
			annotateQuestion(questions[i]);
		}

		assertEquals(2 * questions.length, myCacheResponse.getNumberOfExecutedRequests());
	}

	private void annotateQuestion(String givenQuestion) throws UnsupportedEncodingException {
		annotateQuestion(dbpediaSpotlightEndpoint, givenQuestion, dbpediaspotlightConfidenceMinimum);
	}

	private void annotateQuestion(String endpoint, String givenQuestion, float confidence)
			throws UnsupportedEncodingException {

		for (int i = 0; i < 5; i++) {
			JsonArray resources = myFetcher.getJsonFromService(givenQuestion, endpoint, confidence);

			// check if anything was found
			assertNotEquals(resources.size(), 0);
		}
	}

}
