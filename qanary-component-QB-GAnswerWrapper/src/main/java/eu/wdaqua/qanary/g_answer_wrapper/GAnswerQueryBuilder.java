package eu.wdaqua.qanary.g_answer_wrapper;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.g_answer_wrapper.messages.GAnswerRequest;
import eu.wdaqua.qanary.g_answer_wrapper.messages.GAnswerResult;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


@Component
/**
 * This Qanary component fetches the SPARQL query for
 * the enriched question from the GAnswer API
 *
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class GAnswerQueryBuilder extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(GAnswerQueryBuilder.class);
    private QanaryUtils myQanaryUtils;
    private final float threshold;
    private final URI endpoint;
    private final RestTemplate myRestTemplate;
    private final String langDefault;
    private final ArrayList<String> supportedLang;
    private final String applicationName;

    @Autowired
    private CacheOfRestTemplateResponse myCacheOfResponses;

    public GAnswerQueryBuilder(//
                               float threshold, //
                               @Qualifier("g_answer.langDefault") String langDefault, //
                               @Qualifier("g_answer.endpoint.language.supported") ArrayList<String> supportedLang, //
                               @Qualifier("g_answer.endpointUrl") URI endpoint, //
                               @Value("${spring.application.name}") final String applicationName, //
                               RestTemplate restTemplate, //
                               CacheOfRestTemplateResponse myCacheOfResponses //
    ) throws URISyntaxException {

        logger.info("supportedLang: {}", supportedLang);

        assert threshold >= 0 : "threshold has to be >= 0: " + threshold;
        assert !(endpoint == null) : //
                "endpointUrl cannot be null: " + endpoint;
        assert !(langDefault == null || langDefault.trim().isEmpty()) : //
                "langDefault cannot be null or empty: " + langDefault;
        assert (langDefault.length() == 2) : //
                "langDefault is invalid (requires exactly 2 characters, e.g., 'en'), was " + langDefault + " (length="
                        + langDefault.length() + ")";
        assert !(supportedLang == null || supportedLang.isEmpty()) : //
                "supportedLang cannot be null or empty: " + supportedLang;
        for (int i = 0; i < supportedLang.size(); i++) {
            assert (supportedLang.get(i).length() == 2) : //
                    "supportedLang is invalid (requires exactly 2 characters, e.g., 'en'), was " + supportedLang.get(i) + " (length=" + supportedLang.get(i).length() + ")";
        }

        this.threshold = threshold;
        this.endpoint = endpoint;
        this.langDefault = langDefault;
        this.supportedLang = supportedLang;
        this.myRestTemplate = restTemplate;
        this.applicationName = applicationName;
        this.myCacheOfResponses = myCacheOfResponses;
    }

    public float getThreshold() {
        return threshold;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getLangDefault() {
        return langDefault;
    }

    public ArrayList<String> getSupportedLang() {
        return supportedLang;
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component, some helping notes w.r.t. the typical 3 steps of implementing a
     * Qanary component are included in the method (you might remove all of them)
     *
     * @throws SparqlQueryFailed
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);

        myQanaryUtils = this.getUtils(myQanaryMessage);
        // TODO retrieve language from Qanary triplestore via commons method
        String lang = null;

        if (lang == null) {
            lang = langDefault;
        }

        if (isLangSuppoerted(lang) == false) {
            logger.warn("lang ({}) is not supported", lang);
            return myQanaryMessage;
        }

        // STEP 1: get the required data from the Qanary triplestore (the global process
        // memory)
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        String questionString = myQanaryQuestion.getTextualRepresentation();

        // STEP 2: enriching of query and fetching data from the gAnswer API
        GAnswerResult result = requestGAnswerWebService(endpoint, questionString, lang);

        // STEP 3: add information to Qanary triplestore
        String sparql = getSparqlInsertQuery(myQanaryQuestion, result);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        return myQanaryMessage;
    }

    protected boolean isLangSuppoerted(String lang) {
        for (int i = 0; i < supportedLang.size(); i++) {
            if (supportedLang.get(i).equals(lang)) {
                return true;
            }
        }

        return false;
    }

    protected GAnswerResult requestGAnswerWebService(URI uri, String questionString, String lang) throws URISyntaxException {
        GAnswerRequest gAnswerRequest = new GAnswerRequest(uri, questionString, lang);

        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

        HttpEntity<JSONObject> response = myRestTemplate.getForEntity(gAnswerRequest.getGAnswerQuestionUrlAsString(), JSONObject.class);

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            logger.warn("request was cached: {}", gAnswerRequest);
        } else {
            logger.info("request was actually executed: {}", gAnswerRequest);
        }

        return new GAnswerResult(response.getBody(), gAnswerRequest.getQuestion(), gAnswerRequest.getGAnswerEndpointUrl(), gAnswerRequest.getLanguage());
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
     * @param myQanaryQuestion
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     */
    private String getSparqlInsertQuery(QanaryQuestion<String> myQanaryQuestion, GAnswerResult result)
            throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {

        String sparql = "" //
                + "PREFIX dbr: <http://dbpedia.org/resource/>" //
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>" //
                + "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
                + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" //
                + "" //
                + "INSERT { " //
                + "GRAPH <" + myQanaryQuestion.getInGraph().toString() + ">  {" //
                + "        ?newAnnotation rdf:type qa:AnnotationOfAnswerSPARQL ." //
                + "        ?newAnnotation oa:hasTarget <" + myQanaryQuestion.getUri().toString() + "> ." //
                + "        ?newAnnotation oa:hasBody \"" + cleanStringForSparqlQuery(result.getSparql()) + "\"^^xsd:string ." // the select query that should compute the answer
                + "        ?newAnnotation qa:score \"" + (float) result.getConfidence() + "\"^^xsd:float ." // confidence
                + "        ?newAnnotation oa:annotatedAt ?time ." //
                + "        ?newAnnotation oa:annotatedBy <urn:qanary:" + this.applicationName + "> ." // identify which component made this annotation
                + "    }" //
                + "}" //
                + "WHERE {" //
                + "    BIND (IRI(str(RAND())) AS ?newAnnotation) ." //
                + "    BIND (now() as ?time) . " //
                + "}";

        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);
        return sparql;
    }
}
