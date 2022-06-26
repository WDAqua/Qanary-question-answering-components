package eu.wdaqua.qanary.tebaqa.wrapper;

import java.io.IOException;

import org.springframework.boot.test.context.TestConfiguration;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;

@TestConfiguration
public class TeBeQaTestConfiguration {
	// define here the current CaffeineCacheManager configuration
	static {
		System.setProperty("tebaqa.endpoint.url", "https://tebaqa.demos.dice-research.org/qa-simple");
		System.setProperty("qanary.webservicecalls.cache.specs",
				"maximumSize=1000,expireAfterAccess=" + TestTeBaQAQueryBuilder.MAX_TIME_SPAN_SECONDS + "s");
	}

	/**
	 * get the defined SPARQL query and remove all control characters (like newline) 
	 * 
	 * @return
	 * @throws IOException
	 */
	protected static String getTestQuery(String testQueryFilename) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResources(testQueryFilename).replaceAll("\\p{Cntrl}", "");
	}

}