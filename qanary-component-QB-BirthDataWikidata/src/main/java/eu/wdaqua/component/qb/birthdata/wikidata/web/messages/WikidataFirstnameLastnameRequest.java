package eu.wdaqua.component.qb.birthdata.wikidata.web.messages;

public class WikidataFirstnameLastnameRequest {

	private String firstname;
	private String lastname;
	
	public WikidataFirstnameLastnameRequest() {
		
	}
	
	public WikidataFirstnameLastnameRequest(String firstname, String lastname) {
		this.firstname = firstname;
		this.lastname = lastname;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}
	
	public String toString() {
		return this.getFirstname() + " " + this.getLastname();
	}
}
