package eu.wdaqua.qanary.g_answer.wrapper.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.net.URI;

public class GAnswerRequest {
    private static final Logger logger = LoggerFactory.getLogger(GAnswerRequest.class);
    @Schema(description = "Endpoint URL of gAnswer API (default is already available)", example = "", required = false)
    private URI gAnswerEndpointUrl;
    @NotBlank
    @Schema(description = "Question for that the results will be fetched from the gGnswer API", example = "What is the capital of Germany?", required = true)
    private String question;
    @Schema(description = "2-character language identifier (e.g., en, de, fr, it, es, pt)", example = "en", required = false)
    private String language;

    public GAnswerRequest() {

    }

    public GAnswerRequest(URI gAnswerEndpointUrl, String question, String language) {
        this.gAnswerEndpointUrl = gAnswerEndpointUrl;
        this.question = question;
        this.language = language;
    }

    public GAnswerRequest(String question, String language) {
        this.question = question;
        this.language = language;
    }

    public GAnswerRequest(String question) {
        this.question = question;
    }

    public URI getGAnswerEndpointUrl() {
        return gAnswerEndpointUrl;
    }

    public void setGAnswerEndpointUrl(URI endpoint) {
        this.gAnswerEndpointUrl = endpoint;
    }

    public String getGAnswerQuestionUrlAsString() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("question", getQuestion());

        UriComponentsBuilder url = UriComponentsBuilder.fromUri(getGAnswerEndpointUrl()).queryParams(parameters);
        logger.info("request to {}", url.toUriString());

        return url.toUriString();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "gAnswerRequest: endpoint=" + this.getGAnswerEndpointUrl() + ", question=" + this.getQuestion() + ", lang=" + this.getLanguage();
    }

    public void replaceNullValuesWithDefaultValues(URI endpointDefault, String langDefault) {

        if (this.getGAnswerEndpointUrl() == null) {
            this.setGAnswerEndpointUrl(endpointDefault);
        }

        if (this.getLanguage() == null || this.getLanguage().isBlank()) {
            this.setLanguage(langDefault);
        }
    }
}
