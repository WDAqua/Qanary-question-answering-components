package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theokanning.openai.Usage;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import org.apache.commons.cli.MissingArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class ChatGPTWrapperMockedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGPTWrapperMockedTest.class);
    MockRestServiceServer mockServer;
    private ChatGPTWrapper chatGPTWrapper;
    private CacheOfRestTemplateResponse myCacheOfResponse = new CacheOfRestTemplateResponse();
    private RestTemplateWithCaching restTemplate = new RestTemplateWithCaching(this.myCacheOfResponse);

    @Autowired
    private WebApplicationContext applicationContext;
    @Autowired
    private Environment env;

    /**
     * initialize local controller enabled for tests
     * @throws MissingArgumentException 
     */
    @BeforeEach
    public void setUp() throws MissingTokenException, URISyntaxException, IOException, OpenApiUnreachableException, MissingArgumentException {
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
        // mock the response of the OpenAI API /v1/models
        this.mockServer.expect(requestTo(env.getProperty("chatgpt.base.url") + env.getProperty("chatGPT.getModels.url")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(ChatGPTTestConfiguration.getStringFromFile("json_response/getModels.json"), MediaType.APPLICATION_JSON));

        this.chatGPTWrapper = new ChatGPTWrapper( //  
                "ChatGPTWrapperMockedTest", // 
                "some-token", // 
                false, // 
                env.getProperty("chatgpt.model"), //
                env.getProperty("chatgpt.base.url"), //
                restTemplate, // 
                myCacheOfResponse // 
        );
    }

    @Test
    void createJsonAnswerTest() {
        CompletionResult completionResult = new CompletionResult();

        ArrayList<CompletionChoice> choices = new ArrayList<>();
        CompletionChoice completionChoiceA = new CompletionChoice();
        completionChoiceA.setText("\n\nThis is indeed a test");
        completionChoiceA.setIndex(0);
        completionChoiceA.setLogprobs(null);
        completionChoiceA.setFinish_reason("length");

        choices.add(completionChoiceA);

        Usage usage = new Usage();
        usage.setPromptTokens(5);
        usage.setCompletionTokens(7);
        usage.setTotalTokens(12);

        completionResult.setId("cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7");
        completionResult.setObject("text_completion");
        completionResult.setCreated(1589478378);
        completionResult.setModel("text-davinci-003");
        completionResult.setChoices(choices);
        completionResult.setUsage(usage);

        JsonObject jsonObject = chatGPTWrapper.creatJsonAnswer(completionResult);

        assertNotNull(jsonObject);
        JsonArray choicesArray = jsonObject.get("choices").getAsJsonArray();

        assertEquals("\n\nThis is indeed a test", choicesArray.get(0).getAsJsonObject().get("text").getAsString());
        assertEquals(0, choicesArray.get(0).getAsJsonObject().get("index").getAsInt());
        assertEquals("length", choicesArray.get(0).getAsJsonObject().get("finish_reason").getAsString());


        // Add another choice and test again

        CompletionChoice completionChoiceB = new CompletionChoice();
        completionChoiceB.setText("\n\nThis is indeed an other test");
        completionChoiceB.setIndex(1);
        completionChoiceB.setLogprobs(null);
        completionChoiceB.setFinish_reason("length");

        choices.add(completionChoiceB);

        jsonObject = chatGPTWrapper.creatJsonAnswer(completionResult);

        assertNotNull(jsonObject);
        choicesArray = jsonObject.get("choices").getAsJsonArray();

        assertEquals("\n\nThis is indeed a test", choicesArray.get(0).getAsJsonObject().get("text").getAsString());
        assertEquals(0, choicesArray.get(0).getAsJsonObject().get("index").getAsInt());
        assertEquals("length", choicesArray.get(0).getAsJsonObject().get("finish_reason").getAsString());

        assertEquals("\n\nThis is indeed an other test", choicesArray.get(1).getAsJsonObject().get("text").getAsString());
        assertEquals(1, choicesArray.get(1).getAsJsonObject().get("index").getAsInt());
        assertEquals("length", choicesArray.get(1).getAsJsonObject().get("finish_reason").getAsString());
    }

    @Test
    void createInsertQueryTest() throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions, IOException, SparqlQueryFailed {
        CompletionResult completionResult = new CompletionResult();

        ArrayList<CompletionChoice> choices = new ArrayList<>();
        CompletionChoice completionChoiceA = new CompletionChoice();
        completionChoiceA.setText("\n\nThis is indeed a test");
        completionChoiceA.setIndex(0);
        completionChoiceA.setLogprobs(null);
        completionChoiceA.setFinish_reason("length");

        choices.add(completionChoiceA);

        Usage usage = new Usage();
        usage.setPromptTokens(5);
        usage.setCompletionTokens(7);
        usage.setTotalTokens(12);

        completionResult.setId("cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7");
        completionResult.setObject("text_completion");
        completionResult.setCreated(1589478378);
        completionResult.setModel("text-davinci-003");
        completionResult.setChoices(choices);
        completionResult.setUsage(usage);

        QanaryQuestion<String> myQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        Mockito.when(myQanaryQuestion.getEndpoint()).thenReturn(new URI(endpointKey));
        Mockito.when(myQanaryQuestion.getInGraph()).thenReturn(new URI(inGraphKey));
        Mockito.when(myQanaryQuestion.getOutGraph()).thenReturn(new URI(outGraphKey));
        Mockito.when(myQanaryQuestion.getUri()).thenReturn(new URI("urn:qanary#qestion"));

        String query = chatGPTWrapper.createInsertQuery(myQanaryQuestion, completionResult);

        assertNotNull(query);
        assertEquals(ChatGPTTestConfiguration.getStringFromFile("queries/insertQuery.rq"), query);
    }

}