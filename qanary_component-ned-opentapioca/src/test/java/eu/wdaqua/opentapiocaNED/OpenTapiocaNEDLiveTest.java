package eu.wdaqua.opentapiocaNED;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.google.gson.JsonArray;
import org.apache.http.client.ClientProtocolException;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import org.apache.http.client.ClientProtocolException;

public class OpenTapiocaNEDLiveTest {

	@Test
	public void testGetJsonFromService() throws ClientProtocolException, IOException {
		String opentapiocaEndpoint = "https://opentapioca.org/api/annotate";
		OpenTapiocaServiceFetcher serviceFetcher = new OpenTapiocaServiceFetcher();
		JsonArray resources;

		// generic
		String question1 = "Where and when was Ada Lovelace born?";
		try {
			resources = serviceFetcher.getJsonFromService(
					question1, opentapiocaEndpoint 
					);
			assertNotNull(resources);
			assertTrue(resources.size() > 0);
		} catch (Exception e) {
			fail();
		}

		// superhero name
		String question2 = "What is the birthplace of Batman?";
		try {
			resources = serviceFetcher.getJsonFromService(
					question2, opentapiocaEndpoint 
					);
			assertNotNull(resources);
			assertTrue(resources.size() > 0);
			// TODO: check for specifc QID
		} catch (Exception e) {
			fail();
		}

		// empty question
		String question3 = "";
		try {
			resources = serviceFetcher.getJsonFromService(
					question3, opentapiocaEndpoint 
					);
			assertNotNull(resources);
			assertTrue(resources.size() == 0);
		} catch (Exception e) {
			fail();
		}
		
		// handle exceptions
		String question4 = null;
		try {
			resources = serviceFetcher.getJsonFromService(
					question4, opentapiocaEndpoint 
					);
			fail();
		} catch (IOException | IllegalArgumentException e) {
			// pass 
		} catch (Exception e) {
			fail();
		}
	}
}
