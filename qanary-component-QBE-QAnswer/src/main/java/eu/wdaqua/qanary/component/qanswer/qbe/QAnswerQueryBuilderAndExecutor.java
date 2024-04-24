package eu.wdaqua.qanary.component.qanswer.qbe;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
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
import org.springframework.web.util.UriComponentsBuilder;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
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
    public final Map<String, URL> knowledgeGraphEndpoints;
    private final String applicationName;
    private QanaryUtils myQanaryUtils;
    private float threshold;
    private URI qanswerEndpoint;
    private RestTemplate myRestTemplate;
    private String langDefault;
    private String knowledgeBaseDefault;
    private String userDefault;

    private final String SELECT_ALL_ANNOTATION_OF_INSTANCE = "/queries/select_all_AnnotationOfInstance.rq";
    private final String INSERT_ONE_ANNOTATION_OF_ANSWER_SPARQL = "/queries/insert_one_AnnotationOfAnswerSPARQL.rq";

    public QAnswerQueryBuilderAndExecutor( //
                                           float threshold, //
                                           @Qualifier("langDefault") String langDefault, //
                                           @Qualifier("knowledgeBaseDefault") String knowledgeBaseDefault, //
                                           @Qualifier("userDefault") String userDefault, //
                                           @Qualifier("endpointUrl") URI qanswerEndpoint, //
                                           @Value("${spring.application.name}") final String applicationName, //
                                           RestTemplateWithCaching restTemplate //
    ) throws URISyntaxException, MalformedURLException {

        assert threshold >= 0 : "threshold has to be >= 0: " + threshold;
        assert !(qanswerEndpoint == null) : //
                "qanswerEndpoint cannot be null: " + qanswerEndpoint;
        assert !(langDefault == null || langDefault.trim().isEmpty()) : //
                "langDefault cannot be null or empty: " + langDefault;
        assert (langDefault.length() == 2) : //
                "langDefault is invalid (requires exactly 2 characters, e.g., 'en'), was " + langDefault + " (length="
                        + langDefault.length() + ")";
        assert !(knowledgeBaseDefault == null || knowledgeBaseDefault.trim().isEmpty()) : //
                "knowledgeBaseDefault cannot be null or empty: " + knowledgeBaseDefault;
        assert !(userDefault == null || userDefault.trim().isEmpty()) : //
                "userDefault cannot be null or empty: " + userDefault;

        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(SELECT_ALL_ANNOTATION_OF_INSTANCE);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(INSERT_ONE_ANNOTATION_OF_ANSWER_SPARQL);

        this.threshold = threshold;
        this.qanswerEndpoint = qanswerEndpoint;
        this.langDefault = langDefault;
        this.knowledgeBaseDefault = knowledgeBaseDefault;
        this.userDefault = userDefault;
        this.myRestTemplate = restTemplate;
        this.applicationName = applicationName;

        // define the names of supported triplestore endpoints
        knowledgeGraphEndpoints = Stream.of( //
                        new AbstractMap.SimpleEntry<>("wikidata", new URL("https://query.wikidata.org/bigdata/namespace/wdq/sparql")), //
                        new AbstractMap.SimpleEntry<>("dbpedia", new URL("https://dbpedia.org/sparql"))) //
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));    //

        logger.debug("RestTemplate: {}", restTemplate);
    }

    public float getThreshold() {
        return threshold;
    }

    public URI getQanswerEndpoint() {
        return qanswerEndpoint;
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

        // STEP 1: get the required data from the Qanary triplestore (the global process
        // memory)
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        String questionString = "";
        try {
          questionString = myQanaryQuestion.getTextualRepresentation(lang);
          logger.info("Using specific textual representation for language {}: {}", lang, questionString);
        } catch (Exception e) {
          logger.warn("Could not retrieve specific textual representation for language {}:\n{}", e.getMessage());
        }
        // only if no language-specific text could be found
        if (questionString.length() == 0){
            try {
                questionString = myQanaryQuestion.getTextualRepresentation();
                logger.info("Using default textual representation {}", questionString);
            } catch (Exception e) {
                logger.warn("Could not retrieve textual representation:\n{}", e.getMessage());
                // stop processing of the question, as it will not work without a question text
                return myQanaryMessage;
            }
        }

        // STEP 2: enriching of query and fetching data from the QAnswer API
        List<NamedEntity> retrievedNamedEntities = getNamedEntitiesOfQuestion(myQanaryQuestion,
                myQanaryQuestion.getInGraph());
        String questionStringWithResources = computeQuestionStringWithReplacedResources(questionString,
                retrievedNamedEntities, threshold);
        QAnswerResult result = requestQAnswerWebService(this.getQanswerEndpoint(), questionStringWithResources, lang, knowledgeBaseId, user);

        // STEP 3: add information to Qanary triplestore
        URI graph = myQanaryQuestion.getOutGraph();
        URI questionUri = myQanaryQuestion.getUri();

        String sparqlImprovedQuestion = getSparqlInsertQueryForImprovedQuestion(graph, questionUri, result);
        logger.debug("created SPARQL query for improved question: {}", sparqlImprovedQuestion);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlImprovedQuestion);

        int index = 0; // only one query expected
        String sparqlQueryCandidate = getSparqlInsertQueryForQueryCandidate(graph, questionUri, result, index);
        logger.debug("created SPARQL query for query candidate: {}", sparqlQueryCandidate);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlQueryCandidate);

        String sparqlAnswerJson = getSparqlInsertQueryForAnswerJson(graph, questionUri, result);
        logger.debug("created SPARQL query for answer json: {}", sparqlAnswerJson);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlAnswerJson);

        String sparqlAnswerType = getSparqlInsertQueryForAnswerType(graph, questionUri, result);
        logger.debug("created SPARQL query for answer type: {}", sparqlAnswerType);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlAnswerType);


        // TODO: result json and type

//        String sparql = getSparqlInsertQuery(myQanaryQuestion, result, knowledgeBaseId);
//        myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        return myQanaryMessage;
    }

    public QAnswerResult requestQAnswerWebService(@RequestBody QAnswerRequest request)
            throws URISyntaxException, MalformedURLException, NoLiteralFieldFoundException {
        return requestQAnswerWebService(request.getQanswerEndpointUrl(), request.getQuestion(), request.getLanguage(),
                request.getKnowledgeBaseId(), request.getUser());
    }

    protected QAnswerResult requestQAnswerWebService(URI uri, String questionString, String lang,
                                                     String knowledgeBaseId, String user) throws URISyntaxException, MalformedURLException, NoLiteralFieldFoundException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Qanary/" + this.getClass().getName());

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("query", questionString);
        parameters.add("lang", lang);
        parameters.add("kb", knowledgeBaseId);
        parameters.add("user", user);

        String urlTemplate = UriComponentsBuilder.fromHttpUrl(
                this.qanswerEndpoint.toURL().toURI().toASCIIString()) //
                .queryParam("query", "{query}") //
                .queryParam("lang", "{lang}") //
                .queryParam("kb", "{kb}") //
                .queryParam("user", "{user}")
                .encode().toUriString();

        logger.info("Created URL template: {}", urlTemplate);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
        logger.warn("request to {} with data {}", uri, request.getBody());

        HttpEntity<JSONObject> response; 
        try {
            response = myRestTemplate.postForEntity(uri, request, JSONObject.class);
            logger.info("QAnswer JSON result for question '{}': {}", questionString,
                    response.getBody().getAsString("questions"));

            logger.info("got response: {}", response.getBody().toString());
        } catch (Exception e) {
            response = null;
            logger.info("post to endpoint not successful: {}", e);
        }

        return new QAnswerResult(response.getBody(),
                questionString, uri, lang, knowledgeBaseId, user);
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

        QuerySolutionMap bindingsForGetAnnotationOfNamedEntities = new QuerySolutionMap();
        bindingsForGetAnnotationOfNamedEntities.add("graph", ResourceFactory.createResource(inGraph.toASCIIString()));
        String sparqlGetAnnotation = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_ANNOTATION_OF_INSTANCE, bindingsForGetAnnotationOfNamedEntities);

        boolean ignored = false;
        Float score;
        int start;
        int end;
        QuerySolution tupel;

        ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlGetAnnotation);
        while (resultset.hasNext()) {
            tupel = resultset.next();
            start = tupel.get("start").asLiteral().getInt();
            end = tupel.get("end").asLiteral().getInt();
            score = null;

            if (tupel.contains("score")) {
                score = tupel.get("score").asLiteral().getFloat();
            }
            URI entityResource = new URI(tupel.get("hasBody").asResource().getURI());

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
     * creates the SPARQL query for inserting the improved question into Qanary triplestore
     *
     * @param graph
     * @param questionUri
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    public String getSparqlInsertQueryForImprovedQuestion(
            URI graph, URI questionUri, QAnswerResult result) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

        logger.warn("get query for Improved Question");
        // the computed answer's SPARQL query needs to be cleaned
        String improvedQuestion = cleanStringForSparqlQuery(result.getQuestion());

        // bind: graph, question, service, improvedQuestionText
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));
        bindings.add("question", ResourceFactory.createResource(questionUri.toASCIIString()));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
        bindings.add("improvedQuestionText", ResourceFactory.createStringLiteral(improvedQuestion));

        String sparql = QanaryTripleStoreConnector.insertAnnotationOfImprovedQuestion(bindings);
        logger.warn("sparql: {}", sparql);

        return sparql;
    }

    /**
     * creates the SPARQL query for inserting the query candidate into Qanary triplestore
     *
     * @param graph
     * @param questionUri
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    public String getSparqlInsertQueryForQueryCandidate(
            URI graph, URI questionUri, QAnswerResult result, int index) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

        QuerySolutionMap bindings = new QuerySolutionMap();
        // use the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(questionUri.toASCIIString()));
        bindings.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createStringLiteral(result.getSparql()));
        bindings.add("confidence", ResourceFactory.createTypedLiteral(result.getConfidence()));
        bindings.add("index", ResourceFactory.createTypedLiteral(index)); // TODO: currently, only one SPARQL is annotated, so index is always 0
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL(bindings);
        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

        return sparql;
    }

    /**
     * creates the SPARQL query for inserting the answer json into Qanary triplestore
     *
     * @param graph
     * @param questionUri
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    public String getSparqlInsertQueryForAnswerJson(
            URI graph, URI questionUri, QAnswerResult result) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

        QuerySolutionMap bindings = new QuerySolutionMap();
        // use the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(questionUri.toASCIIString()));
        // TODO: check content of answer json
        bindings.add("jsonAnswer", ResourceFactory.createStringLiteral(result.getAnswerJsonString())); 
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = QanaryTripleStoreConnector.insertAnnotationOfAnswerJson(bindings);
        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

        return sparql;
    }

    /**
     * creates the SPARQL query for inserting the answer type into Qanary triplestore
     *
     * @param graph
     * @param questionUri
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    public String getSparqlInsertQueryForAnswerType(
            URI graph, URI questionUri, QAnswerResult result) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

        QuerySolutionMap bindings = new QuerySolutionMap();
        // use the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(questionUri.toASCIIString()));
        bindings.add("answerDataType", ResourceFactory.createStringLiteral(result.getType())); 
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = QanaryTripleStoreConnector.insertAnnotationOfAnswerDataType(bindings);
        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

        return sparql;
    }

}
