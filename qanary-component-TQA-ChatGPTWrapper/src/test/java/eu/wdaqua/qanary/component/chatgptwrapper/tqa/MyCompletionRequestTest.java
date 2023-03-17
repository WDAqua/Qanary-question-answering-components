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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class MyCompletionRequestTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QanaryServiceControllerTest.class);
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext applicationContext;

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
        request.setModel("text-davinci-003");
        request.setPrompt("Say this is a test");
        request.setMaxTokens(7);
        request.setTemperature(0d);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("model"));
        assertNotNull(jsonObject.get("prompt"));
        assertNotNull(jsonObject.get("max_tokens"));
        assertNotNull(jsonObject.get("temperature"));
        assertEquals("text-davinci-003", jsonObject.get("model").getAsString());
        assertEquals("Say this is a test", jsonObject.get("prompt").getAsString());
        assertEquals(7, jsonObject.get("max_tokens").getAsInt());
        assertEquals(0d, jsonObject.get("temperature").getAsDouble());

        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void modelTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setModel("some-model");

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("model"));
        assertEquals("some-model", jsonObject.get("model").getAsString());

        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void promptTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setPrompt("some-pormpt");

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("prompt"));
        assertEquals("some-pormpt", jsonObject.get("prompt").getAsString());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void suffixTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setSuffix("some-suffix");

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("suffix"));
        assertEquals("some-suffix", jsonObject.get("suffix").getAsString());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void maxTokensTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setMaxTokens(10);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("max_tokens"));
        assertEquals(10, jsonObject.get("max_tokens").getAsInt());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void temperatureTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setTemperature(12.50);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("temperature"));
        assertEquals(12.50, jsonObject.get("temperature").getAsDouble());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void topPTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setTopP(15.30);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("top_p"));
        assertEquals(15.30, jsonObject.get("top_p").getAsDouble());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void nTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setN(20);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("n"));
        assertEquals(20, jsonObject.get("n").getAsInt());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void streamTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setStream(true);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("stream"));
        assertEquals(true, jsonObject.get("stream").getAsBoolean());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void logprobsTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setLogprobs(23);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("logprobs"));
        assertEquals(23, jsonObject.get("logprobs").getAsInt());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void echoTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setEcho(true);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("echo"));
        assertEquals(true, jsonObject.get("echo").getAsBoolean());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
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

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("stop"));

        JsonArray jsonArray = jsonObject.get("stop").getAsJsonArray();
        assertNotNull(jsonArray);
        assertEquals(3, jsonArray.size());
        assertEquals("list", jsonArray.get(0).getAsString());
        assertEquals("of", jsonArray.get(1).getAsString());
        assertEquals("strings", jsonArray.get(2).getAsString());


        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void presencePenaltyTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setPresencePenalty(26.60);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("presence_penalty"));
        assertEquals(26.60, jsonObject.get("presence_penalty").getAsDouble());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void frequencyPenaltyTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setFrequencyPenalty(28.60);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("frequency_penalty"));
        assertEquals(28.60, jsonObject.get("frequency_penalty").getAsDouble());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("logit_bias"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void logitBiasTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        Map<String, Integer> logitBias = new HashMap<>();
        logitBias.put("test", 1);
        logitBias.put("a-other-test", 2);

        request.setLogitBias(logitBias);

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("logit_bias"));

        JsonObject logitBiasObject = jsonObject.get("logit_bias").getAsJsonObject();
        assertNotNull(logitBiasObject);
        assertEquals(1, logitBiasObject.get("test").getAsInt());
        assertEquals(2, logitBiasObject.get("a-other-test").getAsInt());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("user"));
    }

    @Test
    void userTest() {
        MyCompletionRequest request = new MyCompletionRequest();
        request.setUser("some-user");

        JsonObject jsonObject = request.getAsJsonObject();

        assertNotNull(jsonObject);
        assertNotNull(jsonObject.get("user"));
        assertEquals("some-user", jsonObject.get("user").getAsString());

        assertNull(jsonObject.get("model"));
        assertNull(jsonObject.get("prompt"));
        assertNull(jsonObject.get("suffix"));
        assertNull(jsonObject.get("max_tokens"));
        assertNull(jsonObject.get("temperature"));
        assertNull(jsonObject.get("top_p"));
        assertNull(jsonObject.get("n"));
        assertNull(jsonObject.get("stream"));
        assertNull(jsonObject.get("logprobs"));
        assertNull(jsonObject.get("echo"));
        assertNull(jsonObject.get("stop"));
        assertNull(jsonObject.get("presence_penalty"));
        assertNull(jsonObject.get("frequency_penalty"));
        assertNull(jsonObject.get("logit_bias"));
    }
}
