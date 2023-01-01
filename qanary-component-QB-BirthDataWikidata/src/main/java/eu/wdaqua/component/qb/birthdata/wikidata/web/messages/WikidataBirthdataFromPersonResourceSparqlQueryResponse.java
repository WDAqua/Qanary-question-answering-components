package eu.wdaqua.component.qb.birthdata.wikidata.web.messages;

import org.apache.jena.rdf.model.RDFNode;

public class WikidataBirthdataFromPersonResourceSparqlQueryResponse {
	private final String sparqlQuery;
	private final RDFNode usedResource;

	public WikidataBirthdataFromPersonResourceSparqlQueryResponse(String sparqlQuery, RDFNode wikidataResource) {
		this.sparqlQuery = sparqlQuery;
		this.usedResource = wikidataResource;
	}

	public String getSparqlQuery() {
		return sparqlQuery;
	}

	public String getUsedResource() {
		return this.usedResource.toString();
	}
}
