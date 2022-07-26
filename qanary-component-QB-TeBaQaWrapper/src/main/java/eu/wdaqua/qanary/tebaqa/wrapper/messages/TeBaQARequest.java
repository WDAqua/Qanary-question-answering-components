package eu.wdaqua.qanary.tebaqa.wrapper.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.net.URI;

public class TeBaQARequest {
    private static final Logger logger = LoggerFactory.getLogger(TeBaQARequest.class);
    @Schema(description = "Endpoint URL of TeBaQA API (default is already available)", example = "https://tebaqa.demos.dice-research.org/qa-simple", required = false)
    private URI tebaqaEndpointUrl;
    @NotBlank
    @Schema(description = "Question for that the results will be fetched from the TeBaQA API", example = "What is the capital of Germany?", required = true)
    private String question;
    @Schema(description = "2-character language identifier (e.g., en, de, fr, it, es, pt)", example = "en", required = false)
    private String language;

    public TeBaQARequest() {

    }

    public TeBaQARequest(URI tebaqaEndpointUrl, String question, String language) {
        this.tebaqaEndpointUrl = tebaqaEndpointUrl;
        this.question = question;
        this.language = language;
    }

    public TeBaQARequest(String question, String language) {
        this.question = question;
        this.language = language;
    }

    public TeBaQARequest(String question) {
        this.question = question;
    }

    public URI getTeBaQAEndpointUrl() {
        return tebaqaEndpointUrl;
    }

    public void setTeBaQAEndpointUrl(URI endpoint) {
        this.tebaqaEndpointUrl = endpoint;
    }

    public String getTeBaQAQuestionUrlAsString() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("query", getQuestion());
        parameters.add("lang", getLanguage());

        UriComponentsBuilder url = UriComponentsBuilder.fromUri(getTeBaQAEndpointUrl()).queryParams(parameters);
        logger.info("request to {}", url.toUriString());

        return url.toUriString();
    }

    public URI getTeBaQAQuestionUrlAsURI() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("query", getQuestion());
        parameters.add("lang", getLanguage());

        UriComponentsBuilder url = UriComponentsBuilder.fromUri(getTeBaQAEndpointUrl()).queryParams(parameters);
        logger.info("request to {}", url.toUriString());

        return url.build().toUri();
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
        return "TeBaQARequest: endpoint=" + this.getTeBaQAEndpointUrl() + ", question=" + this.getQuestion() + ", lang=" + this.getLanguage();
    }

    public void replaceNullValuesWithDefaultValues(URI endpointDefault, String langDefault) {

        if (this.getTeBaQAEndpointUrl() == null) {
            this.setTeBaQAEndpointUrl(endpointDefault);
        }

        if (this.getLanguage() == null || this.getLanguage().isBlank()) {
            this.setLanguage(langDefault);
        }
    }
}
