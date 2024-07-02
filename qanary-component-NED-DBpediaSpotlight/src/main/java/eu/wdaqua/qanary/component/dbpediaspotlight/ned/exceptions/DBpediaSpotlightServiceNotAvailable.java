package eu.wdaqua.qanary.component.dbpediaspotlight.ned.exceptions;

/**
 * exception thrown when the DBpedia Spotlight service is not available 
 */
public class DBpediaSpotlightServiceNotAvailable extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * constructor
     * 
     * @param message
     */
	public DBpediaSpotlightServiceNotAvailable(String message) {
        super(message);
    }
}
