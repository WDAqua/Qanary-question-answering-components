package eu.wdaqua.qanary.spotlightNED;

/**
 * takes the DBpedia Spotlight properties from the current environment
 * configuration
 * 
 */
public class DBpediaSpotlightConfiguration {
	private  float confidenceMinimum;
	private  String endpoint;

	public  float getConfidenceMinimum() {
		return confidenceMinimum;
	}

	public  void setConfidenceMinimum(float confidenceMinimum) {
		this.confidenceMinimum = confidenceMinimum;
	}

	public  String getEndpoint() {
		return endpoint;
	}

	public  void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public DBpediaSpotlightConfiguration( //
			float confidenceMinimum, //
			String endpoint //
	) {
		this.setConfidenceMinimum(confidenceMinimum);
		this.setEndpoint(endpoint);
	}

}