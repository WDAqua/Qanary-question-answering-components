package eu.wdaqua.qanary.mypackage;

public class BabelfyConfiguration {
	private String endpoint;
	private String testQuestion;
	private String parameters;

	public BabelfyConfiguration(String endpoint, String testQuestion, String parameters) {
		this.endpoint = endpoint;
		this.testQuestion = testQuestion;
		this.parameters = parameters;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getTestQuestion() {
		return testQuestion;
	}

	public String getParameters() {
		return parameters;
	}
}
