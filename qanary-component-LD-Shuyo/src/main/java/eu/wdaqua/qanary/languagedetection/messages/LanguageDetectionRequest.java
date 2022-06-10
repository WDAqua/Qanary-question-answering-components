package eu.wdaqua.qanary.languagedetection.messages;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class LanguageDetectionRequest {

	@NotBlank
	@Schema(description = "the text that will be classified", example = "What is the capital of Germany?", required = true)
	private String text;
	
	public LanguageDetectionRequest() {
	}

	public LanguageDetectionRequest(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
