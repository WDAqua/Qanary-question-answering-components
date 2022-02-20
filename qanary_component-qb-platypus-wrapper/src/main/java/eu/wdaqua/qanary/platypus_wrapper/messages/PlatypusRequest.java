package eu.wdaqua.qanary.platypus_wrapper.messages;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.net.URI;

public class PlatypusRequest {
    @Schema(description = "Endpoint URL of Platypus API (default is already available)", example = "", required = false)
    private URI platypusEndpointUrl;
    @NotBlank
    @Schema(description = "Question for that the results will be fetched from the Platypus API", example = "What is the capital of Germany?", required = true)
    private String question;
    @Schema(description = "2-character language identifier (e.g., en, de, fr, it, es, pt)", example = "en", required = false)
    private String language;

    public PlatypusRequest() {

    }

    public PlatypusRequest(URI platypusEndpointUrl, String question, String language) {
        this.platypusEndpointUrl = platypusEndpointUrl;
        this.question = question;
        this.language = language;
    }

    public PlatypusRequest(String question, String language) {
        this.question = question;
        this.language = language;
    }

    public PlatypusRequest(String question) {
        this.question = question;
    }

    public URI getPlatypusEndpointUrl() {
        return platypusEndpointUrl;
    }

    public void setPlatypusEndpointUrl(URI endpoint) {
        this.platypusEndpointUrl = endpoint;
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
        return "PlatypusRequest: endpoint=" + this.getPlatypusEndpointUrl() + ", question=" + this.getQuestion()
                + ", lang=" + this.getLanguage();
    }

    public void replaceNullValuesWithDefaultValues(URI endpointDefault, String langDefault) {

        if (this.getPlatypusEndpointUrl() == null) {
            this.setPlatypusEndpointUrl(endpointDefault);
        }

        if (this.getLanguage() == null || this.getLanguage().isBlank()) {
            this.setLanguage(langDefault);
        }
    }
}
