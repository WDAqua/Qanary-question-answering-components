package eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api;

import com.theokanning.openai.model.Model;
import com.theokanning.openai.completion.CompletionResult;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;

import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.messages.OpenAiResponseModel;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MyOpenAiApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyOpenAiApi.class);
    private static String BASE_URL; 
    private static String GET_MODELS_URL;
    private static String GET_MODELS_BY_ID_URL;
    private static String CREATE_COMPLETION_URL;

    private final String token;

    public MyOpenAiApi( // 
            @Value("${tqa.chatgptwrapper.api.key}") String token, // 
            @Value("${chatgpt.api.live.test}") boolean doApiIsAliveCheck, // 
            @Value("${chatgpt.base.url}") String baseUrl //  
    ) throws MissingTokenException, URISyntaxException, OpenApiUnreachableException, MissingArgumentException {
    	BASE_URL = baseUrl; 
        GET_MODELS_URL = BASE_URL + "/v1/models";
        GET_MODELS_BY_ID_URL = BASE_URL + "/v1/models/";
        CREATE_COMPLETION_URL = BASE_URL + "/v1/completions";
        
        
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new MissingArgumentException("OpenAI base URL is not set");
        }
    	
        if (token == null || token.isEmpty()) {
            throw new MissingTokenException("OpenAI API key is not set");
        }

        this.token = token;

        if(doApiIsAliveCheck) {
            doLiveCheck();
        }
    }

    public List<Model> getModels(RestTemplate myRestTemplate, CacheOfRestTemplateResponse myCacheOfResponses) throws URISyntaxException {
        URI uri = new URI(GET_MODELS_URL);
        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

        LOGGER.debug("URL: {}", uri);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        ResponseEntity<OpenAiResponseModel> response = myRestTemplate.exchange(uri, HttpMethod.GET, request, OpenAiResponseModel.class);

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", uri);
        } else {
            LOGGER.info("request was actually executed: {}", uri);
        }

        return response.getBody().getData();
    }

    public Model getModelById(RestTemplate myRestTemplate, CacheOfRestTemplateResponse myCacheOfResponses, String modelId) throws URISyntaxException {
        URI uri = new URI(GET_MODELS_BY_ID_URL + modelId);
        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

        LOGGER.debug("URL: {}", uri);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        ResponseEntity<Model> response = myRestTemplate.exchange(uri, HttpMethod.GET, request, Model.class);

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", uri);
        } else {
            LOGGER.info("request was actually executed: {}", uri);
        }

        return response.getBody();
    }

    public CompletionResult createCompletion(
            RestTemplate myRestTemplate,
            CacheOfRestTemplateResponse myCacheOfResponses,
            MyCompletionRequest completionRequest
    ) throws URISyntaxException {
        URI uri = new URI(CREATE_COMPLETION_URL);
        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

        LOGGER.debug("URL: {}", uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        String body = completionRequest.getAsJsonObject().toString();

        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        ResponseEntity<CompletionResult> response = myRestTemplate.exchange(uri, HttpMethod.POST, request, CompletionResult.class);

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", uri);
        } else {
            LOGGER.info("request was actually executed: {}", uri);
        }

        return response.getBody();
    }

    private void doLiveCheck() throws URISyntaxException, OpenApiUnreachableException {
        URI uri = new URI(GET_MODELS_URL);
        RestTemplate myRestTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        ResponseEntity<OpenAiResponseModel> response = myRestTemplate.exchange(uri, HttpMethod.GET, request, OpenAiResponseModel.class);

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if(200 != response.getStatusCodeValue()) {
            LOGGER.error("OpenAI API is not alive. Status code: {}", response.getStatusCodeValue());
            throw new OpenApiUnreachableException("OpenAI API is not alive. Status code: " + response.getStatusCodeValue());
        }
    }

}
