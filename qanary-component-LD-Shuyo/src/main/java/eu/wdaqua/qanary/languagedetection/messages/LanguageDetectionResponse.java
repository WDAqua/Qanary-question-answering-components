package eu.wdaqua.qanary.languagedetection.messages;

import java.util.List;

import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.languagedetection.LanguageDetection;
import io.swagger.v3.oas.annotations.media.Schema;

public class LanguageDetectionResponse {

	@Schema(description = "the originally given text", example = "What is the capital of Germany?", required = true)
	private String text;
	@Schema(description = "array of language codes, a possible result is [\"null\"]", example = "[en]", required = true)
	private List<String> languages;

	public LanguageDetectionResponse(LanguageDetection myLanguageDetection, String text) throws LangDetectException {
		this.text = text;
		this.languages = myLanguageDetection.getDetectedLanguages(text);
	}

	public String getText() {
		return this.text;
	}

	public List<String> getLanguages() {
		return this.languages;
	}

	@Override
	public String toString() {
		return (this.getClass().getName() + " ( text=" + this.getText() + ", languages=" + this.getLanguages() + ")");
	}

}
