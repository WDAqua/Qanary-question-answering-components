package eu.wdaqua.qanary.component.qanswer.qbe.messages;

import java.net.URI;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class QAnswerRequest {
	@Schema(description = "Endpoint URL of QAnswer API (default is already available)", example = "http://qanswer-core1.univ-st-etienne.fr/api/gerbil", required = false)
	private URI qanswerEndpointUrl;
	@NotBlank
	@Schema(description = "Question for that the results will be fetched from the QAnswer API", example = "What is the capital of Germany?", required = true)
	private String question;
	@Schema(description = "2-character language identifier (e.g., en, de, fr, it, es, pt)", example = "en", required = false)
	private String language;
	@Schema(description = "ID of knowledge graph from that the results should be fetched from QAnswer ('dbpedia' is not supported by QAnswer)", example = "wikidata", required = false)
	private String knowledgeBaseId;

	public QAnswerRequest() {

	}

	public QAnswerRequest(URI qanswerEndpointUrl, String question, String language, String knowledgeBaseId) {
		this.qanswerEndpointUrl = qanswerEndpointUrl;
		this.question = question;
		this.language = language;
		this.knowledgeBaseId = knowledgeBaseId;
	}

	public QAnswerRequest(String question, String language, String knowledgeBaseId) {
		this.question = question;
		this.language = language;
		this.knowledgeBaseId = knowledgeBaseId;
	}

	public URI getQanswerEndpointUrl() {
		return qanswerEndpointUrl;
	}

	public void setQanswerEndpointUrl(URI endpoint) {
		this.qanswerEndpointUrl = endpoint;
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

	public String getKnowledgeBaseId() {
		return knowledgeBaseId;
	}

	public void setKnowledgeBaseId(String knowledgeBaseId) {
		this.knowledgeBaseId = knowledgeBaseId;
	}

	@Override
	public String toString() {
		return "QAnswerRequest: endpoint=" + this.getQanswerEndpointUrl() + ", question=" + this.getQuestion()
				+ ", lang=" + this.getLanguage() + ", knowledgeBaseId=" + this.getKnowledgeBaseId();
	}

	public void replaceNullValuesWithDefaultValues(URI endpointDefault, String langDefault,
			String knowledgeBaseDefault) {

		if (this.getQanswerEndpointUrl() == null) {
			this.setQanswerEndpointUrl(endpointDefault);
		}

		if (this.getLanguage() == null || this.getLanguage().isBlank()) {
			this.setLanguage(langDefault);
		}

		if (this.getKnowledgeBaseId() == null || this.getKnowledgeBaseId().isBlank()) {
			this.setKnowledgeBaseId(knowledgeBaseDefault);
		}
	}

}
