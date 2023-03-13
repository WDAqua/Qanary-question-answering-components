package eu.wdaqua.qanary.component.qanswer.qb;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerRequest;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerResult;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import net.minidev.json.JSONObject;

@Component
/**
 * This Qanary component retrieves the Named Entities from the Qanary
 * triplestore, replaces entities by the entity URIs, and fetches the SPARQL
 * query candidates for the (enriched) question from the QAnswer API
 *
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 */
public class QAnswerQueryBuilderAndSparqlResultFetcher extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(QAnswerQueryBuilderAndSparqlResultFetcher.class);
    private final String applicationName;
    private QanaryUtils myQanaryUtils;
    private float threshold;
    private URI endpoint;
    private RestTemplate myRestTemplate;
    private String langDefault;
    private String knowledgeBaseDefault;
    private String userDefault;
    private final String FILENAME_GET_ANNOTATED_ENTITIES = "/queries/get_annotated_entities.rq";

    public QAnswerQueryBuilderAndSparqlResultFetcher( //
                                                      float threshold, //
                                                      @Qualifier("langDefault") String langDefault, //
                                                      @Qualifier("knowledgeBaseDefault") String knowledgeBaseDefault, //
                                                      @Qualifier("userDefault") String userDefault, //
                                                      @Qualifier("endpointUrl") URI endpoint, //
                                                      @Value("${spring.application.name}") final String applicationName, //
                                                      RestTemplateWithCaching restTemplate //
    ) throws URISyntaxException {

        assert threshold >= 0 : "threshold has to be >= 0: " + threshold;
        assert !(endpoint == null) : //
                "endpointUrl cannot be null: " + endpoint;
        assert !(langDefault == null || langDefault.trim().isEmpty()) : //
                "langDefault cannot be null or empty: " + langDefault;
        assert (langDefault.length() == 2) : //
                "langDefault is invalid (requires exactly 2 characters, e.g., 'en'), " //
                        + "was " + langDefault + " (length=" + langDefault.length() + ")";
        assert !(knowledgeBaseDefault == null || knowledgeBaseDefault.trim().isEmpty()) : //
                "knowledgeBaseDefault cannot be null or empty: " + knowledgeBaseDefault;
        assert !(userDefault == null || userDefault.trim().isEmpty()) : //
                "userDefault cannot be null or empty: " + userDefault;

        this.threshold = threshold;
        this.endpoint = endpoint;
        this.langDefault = langDefault;
        this.knowledgeBaseDefault = knowledgeBaseDefault;
        this.userDefault = userDefault;
        this.myRestTemplate = restTemplate;
        this.applicationName = applicationName;

        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_ANNOTATED_ENTITIES);
        
        logger.debug("RestTemplate: {}", restTemplate);

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
        String user = null;

        if (lang == null) {
            lang = langDefault;
        }

        if (knowledgeBaseId == null) {
            knowledgeBaseId = knowledgeBaseDefault;
        }

        if (user == null) {
            user = userDefault;
        }

        URI endpoint = myQanaryMessage.getEndpoint();

        // STEP 1: get the required data from the Qanary triplestore (the global process
        // memory)
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String questionString = myQanaryQuestion.getTextualRepresentation();
        List<NamedEntity> retrievedNamedEntities = getNamedEntitiesOfQuestion(myQanaryQuestion,
                myQanaryQuestion.getInGraph());

        // STEP 2: enriching of query and fetching SPARQL query candidates from the
        // QAnswer API
        String questionStringWithResources = computeQuestionStringWithReplacedResources(questionString,
                retrievedNamedEntities, threshold);
        QAnswerResult result = requestQAnswerWebService(endpoint, questionStringWithResources, lang, knowledgeBaseId, user);

        // STEP 3: add new information to Qanary triplestore
        String sparql = getSparqlInsertQuery(myQanaryQuestion.getOutGraph(), result);
        logger.debug("created SPARQL query: {}", sparql);
        QanaryTripleStoreConnector connector = myQanaryUtils.getQanaryTripleStoreConnector();
        connector.update(sparql);

        return myQanaryMessage;
    }

    protected QAnswerResult requestQAnswerWebService(URI qanaryApiUri, String questionString, String lang,
                                                     String knowledgeBaseId, String user) throws URISyntaxException, MalformedURLException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Qanary/" + this.getClass().getName());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("question", questionString);
        parameters.put("lang", lang);
        parameters.put("kb", knowledgeBaseId);
        parameters.put("user", user);

        String urlTemplate = UriComponentsBuilder.fromHttpUrl(this.endpoint.toURL().toURI().toASCIIString()) //
                .queryParam("question", "{question}") //
                .queryParam("lang", "{lang}") //
                .queryParam("kb", "{kb}") //
                .queryParam("user", "{user}") //
                .encode().toUriString();

        HttpEntity<JSONObject> response = myRestTemplate.getForEntity(urlTemplate, JSONObject.class, parameters);
        logger.info("QAnswer JSON result for question '{}': {}", questionString,
                response.getBody().getAsString("question"));

        return new QAnswerResult(response.getBody(), questionString, qanaryApiUri, lang, knowledgeBaseId, user);
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

        QuerySolutionMap bindingsForSelectAnnotations = new QuerySolutionMap();
		bindingsForSelectAnnotations.add("GRAPH", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		bindingsForSelectAnnotations.add("QUESTION_URI", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));

		// get the template of the SELECT query
		String sparqlGetAnnotations = this.loadQueryFromFile(FILENAME_GET_ANNOTATED_ENTITIES, bindingsForSelectAnnotations);
		logger.info("SPARQL query: {}", sparqlGetAnnotations);        
        
        boolean ignored = false;
        Float score;
        int start;
        int end;
        QuerySolution tupel;

        QanaryTripleStoreConnector connector = myQanaryUtils.getQanaryTripleStoreConnector();
        ResultSet resultset = connector.select(sparqlGetAnnotations);
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
    
	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException  {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
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
     * <p>
     * data can be retrieved via SPARQL 1.1 from the Qanary triplestore using:
     *
     * <pre>
     *
     * SELECT * FROM <YOURGRAPHURI> WHERE {
     * ?s ?p ?o ;
     * a ?type.
     * VALUES ?t {
     * qa:AnnotationOfAnswerSPARQL qa:SparqlQuery
     * qa:AnnotationOfImprovedQuestion qa:ImprovedQuestion
     * qa:AnnotationAnswer qa:Answer
     * qa:AnnotationOfAnswerType qa:AnswerType
     * }
     * }
     * ORDER BY ?type
     * </pre>
     *
     * @param outgraph
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     */
    String getSparqlInsertQuery(URI outgraph, QAnswerResult result)
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {

        // the computed answer's SPARQL query needs to be cleaned
        String improvedQuestion = cleanStringForSparqlQuery(result.getQuestion());

        int counter = 0; // starts at 0
        String annotationsToBeInserted = "";
        String bindForInsert = "";
        for (JsonObject answer : result.getValues()) {

            logger.debug("{}. QAnswer query candidate: {}", counter, answer.toString());

            annotationsToBeInserted += "" //
                    + "\n" //
                    + "  ?annotationSPARQL" + counter + " a 	qa:AnnotationOfAnswerSPARQL ; \n" //
                    + " 		oa:hasTarget    ?question ; \n" //
                    + " 		oa:hasBody      ?sparql" + counter + " ; \n" //
                    + " 		oa:annotatedBy  ?service ; \n" //
                    + " 		oa:annotatedAt  ?time ; \n" //
                    + " 		qa:score        \"" + answer.get("confidence").getAsDouble() + "\"^^xsd:double . \n" //
                    //
                    + "  ?sparql" + counter + " a              qa:SparqlQuery ; \n" //
                    + "         qa:hasPosition  \"" + counter + "\"^^xsd:nonNegativeInteger ; \n" //
                    + "         rdf:value       \"\"\"" + answer.get("query").getAsString() + "\"\"\"^^xsd:string . \n"; //
            bindForInsert += "  BIND (IRI(str(RAND())) AS ?annotationSPARQL" + counter + ") . \n" //
                    + "  BIND (IRI(str(RAND())) AS ?sparql" + counter + ") . \n"; //

            counter++;
        }

        String sparql = "" //
                + "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" //
                + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" //
                + "INSERT { \n" //
                + "GRAPH <" + outgraph.toASCIIString() + "> { \n" //
                + annotationsToBeInserted //
                //
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
                + "  }\n" // end: GRAPH
                + "}\n" // end: insert
                + "WHERE { \n" //
                + bindForInsert //
                //
                + "  BIND (IRI(str(RAND())) AS ?annotationImprovedQuestion) . \n" //
                + "  BIND (IRI(str(RAND())) AS ?improvedQuestion) . \n" //
                //
                + "  BIND (now() AS ?time) . \n" //
                + "  BIND (<" + outgraph.toASCIIString() + "> AS ?question) . \n" //
                + "  BIND (<urn:qanary:" + this.applicationName + "> AS ?service ) . \n" //
                + "  BIND (\"\"\"" + improvedQuestion + "\"\"\"^^xsd:string  AS ?improvedQuestionText ) . \n" //
                //
                + "} \n"; // end: where

        logger.debug("SPARQL insert for adding data to Qanary triplestore:\n{}", sparql);
        return sparql;
    }

}
