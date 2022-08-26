package eu.wdaqua.qanary.component.sina.qb;

public class NoSinaFileProvidedException extends RuntimeException {

	private static final long serialVersionUID = 473507761065615377L;
	
	public NoSinaFileProvidedException() {
		super("No SINA JAR file provided as application parameter 'sina.jarfilelocation'.");
	} 
}
