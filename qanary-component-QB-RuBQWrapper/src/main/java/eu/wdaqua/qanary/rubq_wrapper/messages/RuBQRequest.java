package eu.wdaqua.qanary.rubq_wrapper.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.net.URI;

public class RuBQRequest {
    private static final Logger logger = LoggerFactory.getLogger(RuBQRequest.class);
    @Schema(description = "Endpoint URL of RuBQ API (default is already available)", example = "", required = false)
    private URI rubqEndpointUrl;
    @NotBlank
    @Schema(description = "Question for that the results will be fetched from the RuBQ API", example = "What is the capital of Germany?", required = true)
    private String question;
    @Schema(description = "2-character language identifier (e.g., en,de,fr,it,es,pt)", example = "en", required = false)
    private String language;

    public RuBQRequest() {

    }

    public RuBQRequest(URI rubqEndpointUrl, String question, String language) {
        this.rubqEndpointUrl = rubqEndpointUrl;
        this.question = question;
        this.language = language;
    }

    public RuBQRequest(String question, String language) {
        this.question = question;
        this.language = language;
    }

    public RuBQRequest(String question) {
        this.question = question;
    }

    public URI getRuBQEndpointUrl() {
        return rubqEndpointUrl;
    }

    public void setRuBQEndpointUrl(URI endpoint) {
        this.rubqEndpointUrl = endpoint;
    }

    public String getRuBQQuestionUrlAsString() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("question", getQuestion());

        UriComponentsBuilder url = UriComponentsBuilder.fromUri(getRuBQEndpointUrl()).queryParams(parameters);
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
        return "RuBQRequest: endpoint=" + this.getRuBQEndpointUrl() + ", question=" + this.getQuestion()
                + ", lang=" + this.getLanguage();
    }

    public void replaceNullValuesWithDefaultValues(URI endpointDefault, String langDefault) {

        if (this.getRuBQEndpointUrl() == null) {
            this.setRuBQEndpointUrl(endpointDefault);
        }

        if (this.getLanguage() == null || this.getLanguage().isBlank()) {
            this.setLanguage(langDefault);
        }
    }
}
