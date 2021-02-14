package eu.wdaqua.qanary.component.qanswer.qbe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.NoLiteralFieldFoundException;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.QAnswerRequest;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.QAnswerResult;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import net.minidev.json.JSONObject;

@Component
/**
 * This Qanary component retrieves the Named Entities from the Qanary
 * triplestore, replaces entities by the entity URIs, and fetches results for
 * the enriched question from the QAnswer API
 * 
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 */
public class QAnswerQueryBuilderAndExecutor extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QAnswerQueryBuilderAndExecutor.class);
	private QanaryUtils myQanaryUtils;
	private float threshold;
	private URI endpoint;
	private RestTemplate myRestTemplate;
	private String langDefault;
	private String knowledgeBaseDefault;
	private final String applicationName;

	public QAnswerQueryBuilderAndExecutor( //
			float threshold, //
			@Qualifier("langDefault") String langDefault, //
			@Qualifier("knowledgeBaseDefault") String knowledgeBaseDefault, //
			@Qualifier("endpointUrl") URI endpoint, //
			@Value("${spring.application.name}") final String applicationName, //
			RestTemplate restTemplate //
	) throws URISyntaxException {

		assert threshold >= 0 : "threshold has to be >= 0: " + threshold;
		assert !(endpoint == null) : //
				"endpointUrl cannot be null: " + endpoint;
		assert !(langDefault == null || langDefault.trim().isEmpty()) : //
				"langDefault cannot be null or empty: " + langDefault;
		assert (langDefault.length() == 2) : //
				"langDefault is invalid (requires exactly 2 characters, e.g., 'en'), was " + langDefault + " (length="
						+ langDefault.length() + ")";
		assert !(knowledgeBaseDefault == null || knowledgeBaseDefault.trim().isEmpty()) : //
				"knowledgeBaseDefault cannot be null or empty: " + knowledgeBaseDefault;

		this.threshold = threshold;
		this.endpoint = endpoint;
		this.langDefault = langDefault;
		this.knowledgeBaseDefault = knowledgeBaseDefault;
		this.myRestTemplate = restTemplate;
		this.applicationName = applicationName;
	}

	public float getThreshold() {
		return threshold;
	}

	public URI getEndpoint() {
		return endpoint;
	}

	/**
	 * starts the annotation process
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		myQanaryUtils = this.getUtils(myQanaryMessage);

		String lang = null;
		String knowledgeBaseId = null;

		if (lang == null) {
			lang = langDefault;
		}

		if (knowledgeBaseId == null) {
			knowledgeBaseId = knowledgeBaseDefault;
		}

		// STEP 1: get the required data from the Qanary triplestore (the global process
		// memory)
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
		String questionString = myQanaryQuestion.getTextualRepresentation();
		List<NamedEntity> retrievedNamedEntities = getNamedEntitiesOfQuestion(myQanaryQuestion,
				myQanaryQuestion.getInGraph());

		// STEP 2: enriching of query and fetching data from the QAnswer API
		String questionStringWithResources = computeQuestionStringWithReplacedResources(questionString,
				retrievedNamedEntities, threshold);
		QAnswerResult result = requestQAnswerWebService(endpoint, questionStringWithResources, lang, knowledgeBaseId);

		// STEP 3: add information to Qanary triplestore
		String sparql = getSparqlInsertQuery(myQanaryQuestion, result);
		myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint());

		return myQanaryMessage;
	}

	public QAnswerResult requestQAnswerWebService(@RequestBody QAnswerRequest request)
			throws URISyntaxException, NoLiteralFieldFoundException {
		return requestQAnswerWebService(request.getQanswerEndpointUrl(), request.getQuestion(), request.getLanguage(),
				request.getKnowledgeBaseId());
	}

	protected QAnswerResult requestQAnswerWebService(URI uri, String questionString, String lang,
			String knowledgeBaseId) throws URISyntaxException, NoLiteralFieldFoundException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("User-Agent", "Qanary/" + this.getClass().getName() );
		
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
		parameters.add("query", questionString);
		parameters.add("lang", lang);
		parameters.add("kb", knowledgeBaseId);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
		logger.warn("request to {} with data {}", uri, request.getBody());

		HttpEntity<JSONObject> response = myRestTemplate.postForEntity(uri, request, JSONObject.class);
		logger.info("QAnswer JSON result for question '{}': {}", questionString,
				response.getBody().getAsString("questions"));

		return new QAnswerResult(response.getBody(), questionString, uri, lang, knowledgeBaseId);
	}

	/**
	 * computed list of named entities that are already recognized
	 * 
	 * @param myQanaryQuestion
	 * @param inGraph
	 * @return
	 * @throws Exception
	 */
	protected List<NamedEntity> getNamedEntitiesOfQuestion(QanaryQuestion<String> myQanaryQuestion, URI inGraph)
			throws Exception {
		LinkedList<NamedEntity> namedEntities = new LinkedList<>();

		String sparqlGetAnnotation = "" //
				+ "PREFIX dbr: <http://dbpedia.org/resource/> \n" //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" //
				+ "SELECT ?entityResource ?annotationScore ?start ?end " //
				+ "FROM <" + inGraph.toString() + "> \n" //
				+ "WHERE { \n" //
				+ "    ?annotation    	oa:hasBody   	?entityResource .\n" //
				+ "    ?annotation 		oa:hasTarget 	?target .\n" //
				+ "    ?target 			oa:hasSource    <" + myQanaryQuestion.getUri().toString() + "> .\n" //
				+ "    ?target     		oa:hasSelector  ?textSelector .\n" //
				+ "    ?textSelector 	rdf:type    	oa:TextPositionSelector .\n" //
				+ "    ?textSelector  	oa:start    	?start .\n" //
				+ "    ?textSelector  	oa:end      	?end .\n" //
				+ "    OPTIONAL { \n" //
				+ "			?annotation qa:score	?annotationScore . \n" // we cannot be sure that a score is provided
				+ "	   }\n" //
				+ "}\n";

		boolean ignored = false;
		Float score;
		int start;
		int end;
		QuerySolution tupel;

		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparqlGetAnnotation);
		while (resultset.hasNext()) {
			tupel = resultset.next();
			start = tupel.get("start").asLiteral().getInt();
			end = tupel.get("end").asLiteral().getInt();
			score = null;

			if (tupel.contains("annotationScore")) {
				score = tupel.get("annotationScore").asLiteral().getFloat();
			}
			URI entityResource = new URI(tupel.get("entityResource").asResource().getURI());

			if (score == null || score >= threshold) {
				namedEntities.add(new NamedEntity(entityResource, start, end, score));
				ignored = false;
			} else {
				ignored = true;
			}
			logger.info("found entity in Qanary triplestore: position=({},{}) (score={}>={}) ignored={}", start, end,
					score, threshold, ignored);
		}

		logger.info("Result list ({} items) of getNamedEntitiesOfQuestion for question \"{}\".", namedEntities.size(),
				myQanaryQuestion.getTextualRepresentation());
		if (namedEntities.size() == 0) {
			logger.warn("no named entities exist for '{}'", myQanaryQuestion.getTextualRepresentation());
		} else {
			for (NamedEntity namedEntity : namedEntities) {
				logger.info("found namedEntity: {}", namedEntity.toString());
			}
		}
		return namedEntities;
	}

	/**
	 * create a QAnswer-compatible format of the question
	 * 
	 * @param questionString
	 * @param retrievedNamedEntities
	 * @param threshold
	 * @return
	 */
	protected String computeQuestionStringWithReplacedResources(String questionString,
			List<NamedEntity> retrievedNamedEntities, float threshold) {
		Collections.reverse(retrievedNamedEntities); // list should contain last found entities first
		String questionStringOriginal = questionString;
		int run = 0;
		String first;
		String second, secondSafe;
		String entity;

		for (NamedEntity myNamedEntity : retrievedNamedEntities) {
			// replace String by URL
			if (myNamedEntity.getScore() >= threshold) {
				first = questionString.substring(0, myNamedEntity.getStartPosition());
				second = questionString.substring(myNamedEntity.getEndPosition());
				entity = questionString.substring(myNamedEntity.getStartPosition(), myNamedEntity.getEndPosition());

				// ensure that the next character in the second part is a whitespace to prevent
				// problems with the inserted URIs
				if (!second.startsWith(" ") && !second.isEmpty()) {
					secondSafe = " " + second;
				} else {
					secondSafe = second;
				}
				questionString = first + myNamedEntity.getNamedEntityResource().toASCIIString() + secondSafe;

				logger.debug("{}. replace of '{}' at ({},{}) results in: {}, first:|{}|, second:|{}|", run, entity,
						myNamedEntity.getStartPosition(), myNamedEntity.getEndPosition(), questionString, first,
						second);
				run++;
			}
		}
		logger.info("Question original: {}", questionStringOriginal);
		logger.info("Question changed : {}", questionString);

		return questionString;
	}

	private String cleanStringForSparqlQuery(String myString) {
		return myString.replaceAll("\"", "\\\"").replaceAll("\n", "");
	}

	/**
	 * creates the SPARQL query for inserting the data into Qanary triplestore
	 * 
	 * data can be retrieved via SPARQL 1.1 from the Qanary triplestore using:
	 * 
	 * <pre>
	 *  
	 	SELECT * FROM <YOURGRAPHURI> WHERE {
	 		?s ?p ?o ; 
	 		    a ?type. 
	 		VALUES ?t { 
	 			qa:AnnotationOfAnswerSPARQL qa:SparqlQuery
	 			qa:AnnotationOfImprovedQuestion qa:ImprovedQuestion 
	 			qa:AnnotationAnswer qa:Answer 
	 			qa:AnnotationOfAnswerType qa:AnswerType 
	 		}
	 	}
	 	ORDER BY ?type
	 * </pre>
	 * 
	 * @param myQanaryQuestion
	 * @param result
	 * @return
	 * @throws QanaryExceptionNoOrMultipleQuestions
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 */
	private String getSparqlInsertQuery(QanaryQuestion<String> myQanaryQuestion, QAnswerResult result)
			throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {

		// the computed answer's SPARQL query needs to be cleaned
		String createdAnswerSparqlQuery = cleanStringForSparqlQuery(result.getSparql());
		String improvedQuestion = cleanStringForSparqlQuery(result.getQuestion());

		String answerValuesAsListFormat = "";
		int counter = 1; // starts at 1
		for (String answer : result.getValues()) {
			// only one consistent type for all answers is expected
			if (result.isAnswerOfResourceType()) {
				answerValuesAsListFormat += String.format("; rdf:_%d <%s> ", counter, answer);
			} else {
				answerValuesAsListFormat += String.format("; rdf:_%d \"%s\"^^<%s> ", counter, answer,
						result.getDatatype());
			}
			counter++;
		}

		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" //
				+ "INSERT { \n" //
				+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { \n" //
				// used SPARQL query
				+ "  ?annotationSPARQL a 	qa:AnnotationOfAnswerSPARQL ; \n" //
				+ " 		oa:hasTarget    ?question ; \n" //
				+ " 		oa:hasBody      ?sparql ; \n" //
				+ " 		oa:annotatedBy  ?service ; \n" //
				+ " 		oa:annotatedAt  ?time ; \n" //
				+ " 		qa:score        ?score . \n" //
				//
				+ "  ?sparql a              qa:SparqlQuery ; \n" //
				+ "         rdf:value       ?sparqlQueryString . \n" //
				// improved question
				+ "  ?annotationImprovedQuestion  a 	qa:AnnotationOfImprovedQuestion ; \n" //
				+ " 		oa:hasTarget    ?question ; \n" //
				+ " 		oa:hasBody      ?improvedQuestion ; \n" //
				+ " 		oa:annotatedBy  ?service ; \n" //
				+ " 		oa:annotatedAt  ?time ; \n" //
				+ " 		qa:score        ?score . \n" //
				//
				+ "  ?improvedQuestion a    qa:ImprovedQuestion ; \n " //
				+ "         rdf:value 		?improvedQuestionText . \n " //
				// answer
				+ "  ?annotationAnswer a    qa:AnnotationAnswer ; \n" //
				+ " 		oa:hasTarget    ?question ; \n" //
				+ " 		oa:hasBody      ?answer ; \n" //
				+ " 		oa:annotatedBy  ?service ; \n" //
				+ " 		oa:annotatedAt  ?time ; \n" //
				+ " 		qa:score        ?score . \n" //
				//
				+ "  ?answer a              qa:Answer ; \n" //
				+ "         rdf:value       [ a rdf:Seq " + answerValuesAsListFormat + " ]  . \n" //
				// answer type
				+ "  ?annotationAnswerType a qa:AnnotationOfAnswerType ; \n" //
				+ " 		oa:hasTarget    ?question ; \n" //
				+ " 		oa:hasBody      ?annotationOfAnswerType ; \n" //
				+ " 		oa:annotatedBy  ?service ; \n" //
				+ " 		oa:annotatedAt  ?time ; \n" //
				+ " 		qa:score        ?score . \n" //
				//
				+ "  ?answerType a          qa:AnswerType ; \n" //
				+ " 		rdf:value       ?answerDataType ; \n" //
				+ "	}\n" // end: graph
				+ "}\n" // end: insert
				+ "WHERE { \n" //
				+ "  BIND (IRI(str(RAND())) AS ?annotationSPARQL) . \n" //
				+ "  BIND (IRI(str(RAND())) AS ?sparql) . \n" //
				//
				+ "  BIND (IRI(str(RAND())) AS ?annotationAnswerType) . \n" //
				+ "  BIND (IRI(str(RAND())) AS ?answerType) . \n" //
				//
				+ "  BIND (IRI(str(RAND())) AS ?annotationAnswer) . \n" //
				+ "  BIND (IRI(str(RAND())) AS ?answer) . \n" //
				//
				+ "  BIND (IRI(str(RAND())) AS ?annotationImprovedQuestion) . \n" //
				+ "  BIND (IRI(str(RAND())) AS ?improvedQuestion) . \n" //
				//
				+ "  BIND (now() AS ?time) . \n" //
				+ "  BIND (<" + myQanaryQuestion.getUri().toASCIIString() + "> AS ?question) . \n" //
				+ "  BIND (\"" + result.getConfidence() + "\"^^xsd:double AS ?score) . \n" //
				+ "  BIND (<urn:qanary:" + this.applicationName + "> AS ?service ) . \n" //
				+ "  BIND (\"" + createdAnswerSparqlQuery + "\"^^xsd:string AS ?sparqlQueryString ) . \n" //
				+ "  BIND (\"" + improvedQuestion + "\"^^xsd:string  AS ?improvedQuestionText ) . \n" //
				+ "  BIND ( <" + result.getDatatype() + "> AS ?answerDataType)" + "} \n";

		logger.info("SPARQL insert for adding data to Qanary triplestore", sparql);
		return sparql;
	}

}
