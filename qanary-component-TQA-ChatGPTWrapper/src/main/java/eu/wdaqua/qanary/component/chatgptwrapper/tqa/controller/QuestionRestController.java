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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

import javax.validation.constraints.NotBlank;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

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
            @Value("${chatgpt.api.key}") String token, //
            @Value("${chatgpt.api.live.test.active}") boolean doApiIsAliveCheck, //
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

    /**
     * This method is not intend to be used.
     */
    @Operation(
            description = "This method is not intend to be used.",
            hidden = true
    )
    @GetMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "This method is not intend to be used.")
    void questionGet() {
        LOGGER.error("GET method not allowed for {} endpoint", ENDPOINT);
    }

    @Operation(
            description = "Used the ChatGPT completions API (https://platform.openai.com/docs/api-reference/completions)"
    )

    @PostMapping(value = ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> questionPost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = QuestionRequest.class
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Minimal example",
                                            description = "Thies is an example of a minimal request body.",
                                            value = "{\"prompt\":\"some question\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Complete example",
                                            description = "Thies is an example of a complete request body, with all possible properties. More information about the properties can be found at https://platform.openai.com/docs/api-reference/completions",
                                            value = "{\"model\":\"text-davinci-003\",\"prompt\":\"some question\",\"suffix\":\"some sufix\",\"maxTokens\":16,\"temperature\":1.0,\"topP\":1.0,\"n\":1,\"stream\":false,\"logprobs\":5,\"echo\":false,\"stop\":[\"stop1\",\"stop2\"],\"presencePenalty\":0.0,\"frequencyPenalty\":0.0,\"bestOf\":1,\"logitBias\":{\"50256\":-100},\"user\":\"WSE\"}"
                                    )
                            }
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody QuestionRequest myQuestionRequest
    ) throws URISyntaxException {

        if (myQuestionRequest.getPrompt() == null || myQuestionRequest.getPrompt().isEmpty()) {
            return new ResponseEntity<>(
                    "The request body needs be like: {\"question\":\"some question\"}",
                    HttpStatus.BAD_REQUEST);
        }

        if (myQuestionRequest.getModel() == null) {
            myQuestionRequest.setModel(this.model);
        }

        MyCompletionRequest completionRequest = new MyCompletionRequest();
        completionRequest.setModel(this.model);
        completionRequest.setPrompt(myQuestionRequest.getPrompt());
        completionRequest.setSuffix(myQuestionRequest.getSuffix());
        completionRequest.setMaxTokens(myQuestionRequest.getMaxTokens());
        completionRequest.setTemperature(myQuestionRequest.getTemperature());
        completionRequest.setTopP(myQuestionRequest.getTopP());
        completionRequest.setN(myQuestionRequest.getN());
        completionRequest.setStream(myQuestionRequest.getStream());
        completionRequest.setLogprobs(myQuestionRequest.getLogprobs());
        completionRequest.setEcho(myQuestionRequest.getEcho());
        completionRequest.setStop(myQuestionRequest.getStop());
        completionRequest.setPresencePenalty(myQuestionRequest.getPresencePenalty());
        completionRequest.setFrequencyPenalty(myQuestionRequest.getFrequencyPenalty());
        completionRequest.setBestOf(myQuestionRequest.getBestOf());
        completionRequest.setLogitBias(myQuestionRequest.getLogitBias());
        completionRequest.setUser(myQuestionRequest.getUser());

        LOGGER.info("Created Settings: {}", completionRequest);

        CompletionResult completionResult = openAiApi.createCompletion(
                myRestTemplate,
                myCacheOfResponses,
                completionRequest
        );

        JsonObject jsonAnswer = new JsonObject();
        JsonArray choices = new JsonArray();

        for (CompletionChoice choice : completionResult.getChoices()) {
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

    /**
     * This method is not intend to be used.
     */
    @Operation(
            description = "This method is not intend to be used.",
            hidden = true
    )
    @PutMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "This method is not intend to be used.")
    void questionPut() {
        LOGGER.error("PUT method not allowed for {} endpoint", ENDPOINT);
    }

    /**
     * This method is not intend to be used.
     */
    @Operation(
            description = "This method is not intend to be used.",
            hidden = true
    )
    @DeleteMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "This method is not intend to be used.")
    void questionDelete() {
        LOGGER.error("DELETE method not allowed for {} endpoint", ENDPOINT);
    }

    public static class QuestionRequest {
        String model;
        @NotBlank
        String prompt;
        String suffix = null;
        Integer maxTokens = 16;
        Double temperature = 1.0;
        Double topP = 1.0;
        Integer n = 1;
        Boolean stream = false;
        Integer logprobs = null;
        Boolean echo = false;
        List<String> stop = null;
        Double presencePenalty = 0.0;
        Double frequencyPenalty = 0.0;
        Integer bestOf = 1;
        Map<String, Integer> logitBias = null;
        String user = null;


        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getTopP() {
            return topP;
        }

        public void setTopP(Double topP) {
            this.topP = topP;
        }

        public Integer getN() {
            return n;
        }

        public void setN(Integer n) {
            this.n = n;
        }

        public Boolean getStream() {
            return stream;
        }

        public void setStream(Boolean stream) {
            this.stream = stream;
        }

        public Integer getLogprobs() {
            return logprobs;
        }

        public void setLogprobs(Integer logprobs) {
            this.logprobs = logprobs;
        }

        public Boolean getEcho() {
            return echo;
        }

        public void setEcho(Boolean echo) {
            this.echo = echo;
        }

        public List<String> getStop() {
            return stop;
        }

        public void setStop(List<String> stop) {
            this.stop = stop;
        }

        public Double getPresencePenalty() {
            return presencePenalty;
        }

        public void setPresencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
        }

        public Double getFrequencyPenalty() {
            return frequencyPenalty;
        }

        public void setFrequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
        }

        public Integer getBestOf() {
            return bestOf;
        }

        public void setBestOf(Integer bestOf) {
            this.bestOf = bestOf;
        }

        public Map<String, Integer> getLogitBias() {
            return logitBias;
        }

        public void setLogitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }
}
