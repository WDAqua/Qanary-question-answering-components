package eu.wdaqua.qanary.component.shuyo.ld.messages;

import com.cybozu.labs.langdetect.LangDetectException;
import eu.wdaqua.qanary.component.shuyo.ld.LanguageDetection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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
