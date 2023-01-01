package eu.wdaqua.component.qb.birthdata.wikidata.web;

import java.io.IOException;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.component.qb.birthdata.wikidata.BirthDataQueryBuilder;
import eu.wdaqua.component.qb.birthdata.wikidata.web.messages.WikidataBirthdataFromFirstnameLastnameSparqlQueryResponse;
import eu.wdaqua.component.qb.birthdata.wikidata.web.messages.WikidataBirthdataFromPersonResourceSparqlQueryResponse;
import eu.wdaqua.component.qb.birthdata.wikidata.web.messages.WikidataFirstnameLastnameRequest;
import eu.wdaqua.component.qb.birthdata.wikidata.web.messages.WikidataPersonRequest;
import io.swagger.v3.oas.annotations.Operation;

@Controller
public class BirthDataQueryBuilderController {
	protected static final String SPARQLBYRESOURCE = "/sparqlQueryByResource";
	protected static final String SPARQLBYFIRSTNAMELASTNAME = "/sparqlQueryByFirstnameLastname";
	private static final Logger logger = LoggerFactory.getLogger(BirthDataQueryBuilderController.class);
	private BirthDataQueryBuilder myBirthDataQueryBuilder;

	public BirthDataQueryBuilderController(BirthDataQueryBuilder myBirthDataQueryBuilder) {
		this.myBirthDataQueryBuilder = myBirthDataQueryBuilder;
	}

	@GetMapping(value = BirthDataQueryBuilderController.SPARQLBYRESOURCE)
	public ResponseEntity<String> getDummyResponse() {
		return new ResponseEntity<>("only POST is supported", HttpStatus.I_AM_A_TEAPOT);
	}

	@PostMapping(value = BirthDataQueryBuilderController.SPARQLBYRESOURCE, produces = "application/json")
	@ResponseBody
	@Operation(summary = "Computes a SPARQL for the given resource to retrieve the birth data from the Wikidata knowledge graph (no execution)", //
			operationId = "getWikidataSparqlQueryForFetchingTheBirthdataOfAPerson", //
			description = "Only the Wikidata resource is required. " //
					+ "Examples: \"http://www.wikidata.org/entity/Q567\" ")
	public WikidataBirthdataFromPersonResourceSparqlQueryResponse getWikidataSparqlQueryForFetchingTheBirthdataOfAPerson(
			@RequestBody WikidataPersonRequest myWikidataPerson) throws IOException {
		logger.info("getWikidataSparqlQueryForFetchingTheBirthdataOfAPerson: {}", myWikidataPerson);
		RDFNode wikidataResource = ResourceFactory.createResource(myWikidataPerson.getResource());
		String sparqlQuery = this.myBirthDataQueryBuilder.createWikidataSparqlQuery(wikidataResource);
		return new WikidataBirthdataFromPersonResourceSparqlQueryResponse(sparqlQuery, wikidataResource);
	}

	@PostMapping(value = BirthDataQueryBuilderController.SPARQLBYFIRSTNAMELASTNAME, produces = "application/json")
	@ResponseBody
	@Operation(summary = "Computes a SPARQL for the given firstname and lastname to retrieve the birth data from the Wikidata knowledge graph (no execution)", //
			operationId = "getWikidataSparqlQueryForFetchingTheBirthdataForFirstnameLastname", //
			description = "Only the firstname and lastname are required that should match to a Wikidata person instance. " //
					+ "Examples: \"Angela\"/\"Merkel\" ")
	public WikidataBirthdataFromFirstnameLastnameSparqlQueryResponse getWikidataSparqlQueryForFetchingTheBirthdataForFirstnameLastname(
			@RequestBody WikidataFirstnameLastnameRequest myFirstnameLastname) throws IOException {
		logger.info("getWikidataSparqlQueryForFetchingTheBirthdataForFirstnameLastname: {}", myFirstnameLastname);
		String sparqlQuery = this.myBirthDataQueryBuilder.createWikidataSparqlQuery(myFirstnameLastname.getFirstname(),
				myFirstnameLastname.getLastname());
		return new WikidataBirthdataFromFirstnameLastnameSparqlQueryResponse(myFirstnameLastname.getFirstname(),
				myFirstnameLastname.getLastname(), sparqlQuery);
	}

}
