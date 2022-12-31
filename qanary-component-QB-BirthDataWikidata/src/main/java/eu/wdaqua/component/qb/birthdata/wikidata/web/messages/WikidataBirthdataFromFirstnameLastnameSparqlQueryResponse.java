package eu.wdaqua.component.qb.birthdata.wikidata.web.messages;

public class WikidataBirthdataFromFirstnameLastnameSparqlQueryResponse {
	private final String sparqlQuery;
	private final String firstname;
	private final String lastname;
	
	public WikidataBirthdataFromFirstnameLastnameSparqlQueryResponse(String firstname, String lastname, String sparqlQuery) {
		this.sparqlQuery = sparqlQuery;
		this.firstname = firstname;
		this.lastname = lastname;
	}

	public String getSparqlQuery() {
		return sparqlQuery;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}
}
