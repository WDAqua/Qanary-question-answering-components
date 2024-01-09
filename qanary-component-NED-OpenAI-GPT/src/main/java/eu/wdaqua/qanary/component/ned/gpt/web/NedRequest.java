package eu.wdaqua.qanary.component.ned.gpt.web;

/**
 * represents a request for identifying Named Entity including its identified
 * resource
 */
public class NedRequest {

	private String sentence;
	private String model;

	public NedRequest() {

	}

	public NedRequest(String sentence, String model) {
		this.sentence = sentence;
		this.model = model;
	}

	public String getSentence() {
		return this.sentence;
	}

	public String getModel() {
		return this.model;
	}

	public String toString() {
		return this.getSentence() + " on " + this.getModel();
	}
}