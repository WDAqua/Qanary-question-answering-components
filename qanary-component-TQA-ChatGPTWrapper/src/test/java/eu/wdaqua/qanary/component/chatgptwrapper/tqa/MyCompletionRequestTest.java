package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyCompletionRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class MyCompletionRequestTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QanaryServiceControllerTest.class);
    private MockMvc mockMvc;
    private static final String PROMPT = "prompt";
    private static final String MODEL = "model";
    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private Environment env;
    /**
     * initialize local controller enabled for tests
     */
    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    @Test
    void multiTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setModel(env.getProperty("chatgpt.model"));
        request.setPrompt("Say this is a test");
        request.setMaxTokens(7);
        request.setTemperature(0d);

        JsonObject jsonObject = request.getAsJsonObject();
        String[] propertiesWithValues = {MODEL, PROMPT, "max_tokens", "temperature"};
        checkPropertiesOfMessage(jsonObject, propertiesWithValues);

        assertEquals(env.getProperty("chatgpt.model"), jsonObject.get(MODEL).getAsString());
        assertEquals("Say this is a test", jsonObject.get(PROMPT).getAsString());
        assertEquals(7, jsonObject.get("max_tokens").getAsInt());
        assertEquals(0d, jsonObject.get("temperature").getAsDouble());

    }

    @Test
    void modelTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setModel("some-model");

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, MODEL);

        assertEquals("some-model", jsonObject.get(MODEL).getAsString());
    }

    @Test
    void promptTest() {
    	String myValue = "some-pormpt";
        MyCompletionRequest request = new MyCompletionRequest();
        request.setPrompt(myValue);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, PROMPT);

        assertEquals(myValue, jsonObject.get(PROMPT).getAsString());
    }

    @Test
    void suffixTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setSuffix("some-suffix");

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "suffix");

        assertEquals("some-suffix", jsonObject.get("suffix").getAsString());
    }

    @Test
    void maxTokensTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setMaxTokens(10);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "max_tokens");

        assertEquals(10, jsonObject.get("max_tokens").getAsInt());
    }

    @Test
    void temperatureTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setTemperature(12.50);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "temperature");
        
        assertEquals(12.50, jsonObject.get("temperature").getAsDouble());

    }

    @Test
    void topPTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setTopP(15.30);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "top_p");

        assertEquals(15.30, jsonObject.get("top_p").getAsDouble());
    }

    @Test
    void nTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setN(20);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "n");

        assertEquals(20, jsonObject.get("n").getAsInt());
    }

    @Test
    void streamTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setStream(true);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "stream");

        assertEquals(true, jsonObject.get("stream").getAsBoolean());
    }

    @Test
    void logprobsTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setLogprobs(23);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "logprobs");

        assertEquals(23, jsonObject.get("logprobs").getAsInt());
    }

    @Test
    void echoTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setEcho(true);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "echo");

        assertEquals(true, jsonObject.get("echo").getAsBoolean());
    }

    @Test
    void stopTest() {
        MyCompletionRequest request = new MyCompletionRequest();

        List<String> someList = new ArrayList<>();
        someList.add("list");
        someList.add("of");
        someList.add("strings");

        request.setStop(someList);
        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "stop");

        JsonArray jsonArray = jsonObject.get("stop").getAsJsonArray();
        assertNotNull(jsonArray);
        assertEquals(3, jsonArray.size());
        assertEquals("list", jsonArray.get(0).getAsString());
        assertEquals("of", jsonArray.get(1).getAsString());
        assertEquals("strings", jsonArray.get(2).getAsString());

    }

    @Test
    void presencePenaltyTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setPresencePenalty(26.60);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "presence_penalty");

        assertEquals(26.60, jsonObject.get("presence_penalty").getAsDouble());
    }

    @Test
    void frequencyPenaltyTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setFrequencyPenalty(28.60);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "frequency_penalty");

        assertEquals(28.60, jsonObject.get("frequency_penalty").getAsDouble());
    }

    @Test
    void logitBiasTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        Map<String, Integer> logitBias = new HashMap<>();
        logitBias.put("test", 1);
        logitBias.put("a-other-test", 2);

        request.setLogitBias(logitBias);

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "logit_bias");

        JsonObject logitBiasObject = jsonObject.get("logit_bias").getAsJsonObject();
        assertNotNull(logitBiasObject);
        assertEquals(1, logitBiasObject.get("test").getAsInt());
        assertEquals(2, logitBiasObject.get("a-other-test").getAsInt());
    }

    @Test
    void userTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setUser("some-user");

        JsonObject jsonObject = request.getAsJsonObject();
        checkPropertiesOfMessage(jsonObject, "user");

        assertEquals("some-user", jsonObject.get("user").getAsString());
    }
    
	/**
	 * check all property for holding NULL values, but the one provided should be
	 * NOT NULL
	 * 
	 * @param jsonObject
	 * @param propertyExpectedToBeNotNull
	 */
	private void checkPropertiesOfMessage(JsonObject jsonObject, String propertyExpectedToBeNotNull) {
		String[] propertiesExpectedToBeNotNull = {propertyExpectedToBeNotNull};
	
		
	}

	private void checkPropertiesOfMessage(JsonObject jsonObject, String[] propertiesExpectedToBeNotNull) {
		String[] properties = { MODEL, PROMPT, "suffix", "max_tokens", "temperature", "top_p", "user", "n", "stream",
				"logprobs", "echo", "stop", "presence_penalty", "frequency_penalty", "logit_bias" };
		List<String> propertiesExpectedToBeNotNullValues = Arrays.asList(propertiesExpectedToBeNotNull);

		assertNotNull(jsonObject);

		for (int i = 0; i < properties.length; i++) {
			String property = properties[i];

			if (propertiesExpectedToBeNotNullValues.contains(property)) {
				assertNotNull(jsonObject.get(property), "Property '" + property + "' should have been NOT NULL.");
			} else {
				assertNull(jsonObject.get(property),
						"Property '" + property + "' should have been NULL, but was: " + jsonObject.get(property));
			}
		}
	}
}
