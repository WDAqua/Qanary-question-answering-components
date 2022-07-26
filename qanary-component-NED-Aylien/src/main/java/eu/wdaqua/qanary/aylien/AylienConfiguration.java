package eu.wdaqua.qanary.aylien;

import org.springframework.stereotype.Component;

@Component
public class AylienConfiguration {

	private String endpoint;
	private String testQuestion;

	public AylienConfiguration(String endpoint, String testQuestion) {
		this.endpoint = endpoint;
		this.testQuestion = testQuestion;
	}

	public String getEndpoint() {
		return this.endpoint;
	}

	public String getTestQuestion() {
		return this.testQuestion;
	}

}
