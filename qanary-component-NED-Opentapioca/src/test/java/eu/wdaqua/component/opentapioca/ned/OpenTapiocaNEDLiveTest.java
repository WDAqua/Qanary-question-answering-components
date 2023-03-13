package eu.wdaqua.component.opentapioca.ned;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * the tests will only be executed if the system environment variable
 * live_tests_enabled is set to true
 * 
 * <pre>
 	export live_tests_enabled=true
 	export live_tests_enabled=false
 	unset live_tests_enabled
 * </pre>
 * 
 * 
 */
class OpenTapiocaNEDLiveTest {
	private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaNEDLiveTest.class);
	private String opentapiocaEndpoint;
	private OpenTapiocaServiceFetcher serviceFetcher;

	private boolean resourcesContainID(JsonArray resources, String id) {
		for (int i = 0; i < resources.size(); i++) {
			JsonObject object = resources.get(i).getAsJsonObject();
			JsonArray tags = object.get("tags").getAsJsonArray();
			for (int j = 0; j < tags.size(); j++) {
				JsonObject entity = tags.get(j).getAsJsonObject();
				String qid = entity.get("id").getAsString();
				if (qid.equals(id))
					return true;
			}
		}
		return false;
	}

	@BeforeEach
	void init() {
		opentapiocaEndpoint = "https://opentapioca.org/api/annotate";
	}

	@Test
	@DisabledIfEnvironmentVariable(named = "live_tests_enabled", matches = "true")
	void testShowStatusOfTestsDisabled() {
		logger.warn(
				"live_tests_enabled==false --> live tests will NOT be executed. Define system env variable live_tests_enabled=true to activate test request to the live API.");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "live_tests_enabled", matches = "true")
	void testShowStatusOfTestsEnabled() {
		logger.warn(
				"live_tests_enabled==true --> live tests will be executed. Define system env variable live_tests_enabled=false to deactivate test request to the live API.");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "live_tests_enabled", matches = "true")
	void testGetJsonFromServiceForPerson() throws ClientProtocolException, IOException {
		String question = "Where and when was Ada Lovelace born?";
		logger.warn("Opentapioca live test: '{}'", question);
		JsonArray resources = serviceFetcher.getJsonFromService(question, opentapiocaEndpoint);
		assertNotNull(resources);
		assertTrue(resources.size() > 0);
		assertTrue(resourcesContainID(resources, "Q7259"));
	}

	@Disabled
	@Test
	// ignored because the currently used opentapioca implementation does not
	// recognize super heroes
	void testGetJsonFromServiceForSuperHero() throws ClientProtocolException, IOException {
		String question = "Aquaman";
		logger.warn("Opentapioca live test: '{}'", question);
		JsonArray resources = serviceFetcher.getJsonFromService(question, opentapiocaEndpoint);
		assertNotNull(resources);
		assertTrue(resources.size() > 0);
		assertTrue(resourcesContainID(resources, "Q623059"));
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "live_tests_enabled", matches = "true")
	void testGetJsonFromServiceWithEmptyQuestion() throws ClientProtocolException, IOException {
		String question = "";
		logger.warn("Opentapioca live test: '{}'", question);
		JsonArray resources = serviceFetcher.getJsonFromService(question, opentapiocaEndpoint);
		assertNotNull(resources);
		assertTrue(resources.size() == 0);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "live_tests_enabled", matches = "true")
	void testInvalidQuestionException() throws ClientProtocolException, IOException {
		String question = null;
		logger.warn("Opentapioca live test: '{}'", question);
		try {
			serviceFetcher.getJsonFromService(question, opentapiocaEndpoint);
			fail("null value question String should not be processed");
		} catch (IOException | IllegalArgumentException e) {
			// pass
		} catch (Exception e) {
			fail("Unexpected Exception");
		}
	}
}
