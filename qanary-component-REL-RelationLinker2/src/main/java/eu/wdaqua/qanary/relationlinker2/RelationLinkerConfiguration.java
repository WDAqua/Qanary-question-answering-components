package eu.wdaqua.qanary.relationlinker2;

public class RelationLinkerConfiguration {
	private String endpoint;
	private String testQuestion;

	public RelationLinkerConfiguration( String endpoint, String testQuestion) {
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
