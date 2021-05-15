package eu.wdaqua.opentapiocaNED;

/**
 *
 * takes the Open Tapioca properties from the current environment configuration
 *
 */
public class OpenTapiocaConfiguration {
	private String endpoint;

	public OpenTapiocaConfiguration(String endpoint) {
		this.setEndpoint(endpoint);
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
}
