package eu.wdaqua.component.qb.birthdata.wikidata.web.messages;

public class WikidataPersonRequest {

	private String resource;

	public WikidataPersonRequest() {
		
	}
	
	public WikidataPersonRequest(String resource) {
		this.resource = resource;
	}
	
	public String getResource() {
		return this.resource;
	}

	public String toString() {
		return this.getResource();
	}
}
