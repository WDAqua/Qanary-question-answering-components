package eu.wdaqua.qanary.component.sina.qb;

public class NoSinaFileNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -7683640509974322741L;

	public NoSinaFileNotFoundException(String givenFileLocation) {
		super("Sina file not found at " + givenFileLocation + ".");
	}
	
}
