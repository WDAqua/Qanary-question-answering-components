package eu.wdaqua.qanary.component.chatgptwrapper.tqa.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyCompletionRequest;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyOpenAiApi;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

@RestController
@CrossOrigin(origins = "${question_controller.corss.origin}")
public class QuestionRestController {
    public static final String ENDPOINT = "/question";
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionRestController.class);

    private final RestTemplate myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;

    private MyOpenAiApi openAiApi;
    private String model;
    public QuestionRestController(
            @Value("${tqa.chatgptwrapper.api.key}") String token, // 
            @Value("${chatgpt.api.live.test}") boolean doApiIsAliveCheck, // 
            @Value("${chatgpt.model}") String model, //
            @Value("${chatgpt.base.url}") String endpointUrl, //              
            @Autowired RestTemplate restTemplate, // 
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses //
    ) throws OpenApiUnreachableException, MissingTokenException, URISyntaxException, MissingArgumentException {
        this.myRestTemplate = restTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.openAiApi = new MyOpenAiApi(token, doApiIsAliveCheck, endpointUrl);
        this.model = model;
    }

    @GetMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
    void questionGet() {
        LOGGER.error("GET method not allowed for {} endpoint", ENDPOINT);
    }

    @PostMapping(value = ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> questionPost(@RequestBody QuestionRequest myQuestionRequest) throws URISyntaxException {

        if(myQuestionRequest.getQuestion() == null || myQuestionRequest.getQuestion().isEmpty()) {
            return new ResponseEntity<>(
                    "The request body needs be like: {\"question\":\"some question\"}",
                    HttpStatus.BAD_REQUEST);
        }

        MyCompletionRequest completionRequest = new MyCompletionRequest();
        completionRequest.setModel(model);
        completionRequest.setPrompt(myQuestionRequest.getQuestion());

        CompletionResult completionResult = openAiApi.createCompletion(
                myRestTemplate,
                myCacheOfResponses,
                completionRequest
        );

        JsonObject jsonAnswer = new JsonObject();
        JsonArray choices = new JsonArray();

        for(CompletionChoice choice : completionResult.getChoices()) {
            JsonObject choiceJson = new JsonObject();

            if (choice.getText() != null) {
                choiceJson.addProperty("text", choice.getText());
            }
            if (choice.getIndex() != null) {
                choiceJson.addProperty("index", choice.getIndex());
            }
            if (choice.getLogprobs() != null) {
                choiceJson.addProperty("logprobs", choice.getLogprobs().toString());
            }
            if (choice.getFinish_reason() != null) {
                choiceJson.addProperty("finish_reason", choice.getFinish_reason());
            }

            choices.add(choiceJson);
        }

        jsonAnswer.add("choices", choices);

        return new ResponseEntity<>(jsonAnswer.toString(), HttpStatus.OK);
    }

    @PutMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
    void questionPut() {
        LOGGER.error("PUT method not allowed for {} endpoint", ENDPOINT);
    }

    @DeleteMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
    void questionDelete() {
        LOGGER.error("DELETE method not allowed for {} endpoint", ENDPOINT);
    }

    public record QuestionRequest(String question) {
        public String getQuestion() {
            return question;
        }
    }
}
