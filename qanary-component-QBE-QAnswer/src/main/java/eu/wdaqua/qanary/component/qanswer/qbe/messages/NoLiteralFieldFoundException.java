package eu.wdaqua.qanary.component.qanswer.qbe.messages;

public class NoLiteralFieldFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public NoLiteralFieldFoundException(String type) {
		super("'" + type + "' found, but should have been 'literal'.");
	}
}
