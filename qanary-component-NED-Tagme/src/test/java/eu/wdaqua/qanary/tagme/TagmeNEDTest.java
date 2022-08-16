package eu.wdaqua.qanary.tagme;

import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class TagmeNEDTest {
	private static final Logger logger = LoggerFactory.getLogger(TagmeNEDTest.class);

	@Autowired
	private Environment env;

	public String tagMeServiceURL;
	private float tagMeThreshold = 0.1f; // keep it low to ensure some data is useable
	private TagmeNED myTagmeNED;

	@BeforeEach
	void init() {
		tagMeServiceURL = env.getProperty("ned-tagme.service.url");
		logger.info("tagMeServiceURL: {}", tagMeServiceURL);
		assertNotNull(tagMeServiceURL, "tagMeServiceURL is not allowed to be null.");
		myTagmeNED = new TagmeNED("test", false, "none", tagMeServiceURL, tagMeThreshold);
	}

	@Test
	void testWhereAreLondonAndGermany()
			throws UnsupportedEncodingException, ClientProtocolException, IOException {
		List<TestData> myTestData = new LinkedList<>();

		List<NamedEntity> entities0 = new LinkedList<>();
		myTestData.add(new TestData("Where are London and Germany?", entities0));
		entities0.add(new NamedEntity("http://dbpedia.org/resource/London", 10, 17));
		entities0.add(new NamedEntity("http://dbpedia.org/resource/Germany", 21, 29));

		for (TestData myData : myTestData) {
			List<NamedEntity> result = myTagmeNED.retrieveDataFromWebService(myData.getQuestion());

			// check if expected named entites are contained in result
			for (NamedEntity expectedNamedEntity : myData.getNamedEntities()) {
				assertTrue(isExpectedNamedEntityContained(result, expectedNamedEntity),
						expectedNamedEntity + " was not found in Web service result:\n" + getAllResults(result));
			}
		}
	}

	@Test
	void testWhenWasAlbertEinsteinBorn()
			throws UnsupportedEncodingException, ClientProtocolException, IOException {
		List<TestData> myTestData = new LinkedList<>();

		List<NamedEntity> entities0 = new LinkedList<>();
		myTestData.add(new TestData("When was Albert Einstein born?", entities0));
		entities0.add(new NamedEntity("http://dbpedia.org/resource/Albert_Einstein", 9, 25));

		for (TestData myData : myTestData) {
			List<NamedEntity> result = myTagmeNED.retrieveDataFromWebService(myData.getQuestion());

			// check if expected named entites are contained in result
			for (NamedEntity expectedNamedEntity : myData.getNamedEntities()) {
				assertTrue(isExpectedNamedEntityContained(result, expectedNamedEntity),
						expectedNamedEntity + " was not found in Web service result:\n" + getAllResults(result));
			}
		}
	}

	/**
	 * check if the list of given named entities contains an expected named entity
	 * 
	 * @param result
	 * @param expectedNamedEntity
	 * @return
	 */
	private boolean isExpectedNamedEntityContained(List<NamedEntity> result, NamedEntity expectedNamedEntity) {
		for (NamedEntity computedNamedEntity : result) {
			if (expectedNamedEntity.getBegin() == computedNamedEntity.getBegin() //
					&& expectedNamedEntity.getEnd() == computedNamedEntity.getEnd() //
					&& expectedNamedEntity.getLink().compareTo(computedNamedEntity.getLink()) == 0) {
				return true;
			} else {
				logger.debug("not found: {} != {}", expectedNamedEntity, computedNamedEntity);
			}
		}
		return false;
	}

	private String getAllResults(List<NamedEntity> result) {
		String printable = "";
		for (NamedEntity myNamedEntity : result) {
			printable += myNamedEntity + "\n";
		}
		return printable;
	}

}

class TestData {
	private String question;
	private List<NamedEntity> entities;

	public TestData(String question, List<NamedEntity> entities) {
		this.question = question;
		this.entities = entities;
	}

	String getQuestion() {
		return this.question;
	}

	List<NamedEntity> getNamedEntities() {
		return this.entities;
	}
}
