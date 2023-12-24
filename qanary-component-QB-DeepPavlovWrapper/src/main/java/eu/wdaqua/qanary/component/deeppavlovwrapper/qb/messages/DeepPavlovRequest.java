package eu.wdaqua.qanary.component.deeppavlovwrapper.qb.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.net.URI;

public class DeepPavlovRequest {
    private static final Logger logger = LoggerFactory.getLogger(DeepPavlovRequest.class);
    @Schema(description = "Endpoint URL of DeepPavlov API (default is already available)", example = "", required = false)
    private URI deepPavlovEndpointUrl;
    @NotBlank
    @Schema(description = "Question for that the results will be fetched from the DeepPavlov API", example = "What is the capital of Germany?", required = true)
    private String question;
    @Schema(description = "2-character language identifier (e.g., en, de, fr, it, es, pt)", example = "en", required = true)
    private String language;

    public DeepPavlovRequest() {

    }

    public DeepPavlovRequest(URI deepPavlovEndpointUrl, String question, String language) {
        this.deepPavlovEndpointUrl = deepPavlovEndpointUrl;
        this.question = question;
        this.language = language;
    }

    public DeepPavlovRequest(String question, String language) {
        this.question = question;
        this.language = language;
    }

    public URI getDeepPavlovEndpointUrl() {
        return deepPavlovEndpointUrl;
    }

    public void setDeepPavlovEndpointUrl(URI endpoint) {
        this.deepPavlovEndpointUrl = endpoint;
    }

    public String getDeepPavlovQuestionUrlAsString() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("question", getQuestion());
        parameters.add("lang", getLanguage());

        UriComponentsBuilder url = UriComponentsBuilder.fromUri(
                getDeepPavlovEndpointUrl()).queryParams(parameters);
        logger.info("request to {}", url.toUriString());

        return url.toUriString();

    }

    public String getQuestion() {
        return question;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "deepPavlovRequest: endpoint=" + this.getDeepPavlovEndpointUrl() 
            + ", question=" + this.getQuestion()
            + ", lang=" + this.getLanguage();
    }

}
