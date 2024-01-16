package eu.wdaqua.qanary.component.ned.gpt.openai;

public class OpenAiApiFetchingServiceFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	public OpenAiApiFetchingServiceFailedException(String message) {
		super(message);
	}

}
