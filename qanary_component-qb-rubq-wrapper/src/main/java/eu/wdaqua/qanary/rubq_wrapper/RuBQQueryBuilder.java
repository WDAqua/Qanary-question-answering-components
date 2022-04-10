package eu.wdaqua.qanary.rubq_wrapper;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.rubq_wrapper.messages.RuBQRequest;
import eu.wdaqua.qanary.rubq_wrapper.messages.RuBQResult;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


@Component
/**
 * This Qanary component fetches the SPARQL query for
 * the enriched question from the RuBQ API
 *
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class RuBQQueryBuilder extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(RuBQQueryBuilder.class);
    private QanaryUtils myQanaryUtils;
    private float threshold;
    private URI endpoint;
    private RestTemplate myRestTemplate;
    private String langDefault;
    private ArrayList<String> supportedLang;
    private final String applicationName;

    public RuBQQueryBuilder(//
                            float threshold, //
                            @Qualifier("rubq.langDefault") String langDefault, //
                            @Qualifier("rubq.langDefault") ArrayList<String> supportedLang, //
                            @Qualifier("rubq.endpointUrl") URI endpoint, //
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
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
        String questionString = myQanaryQuestion.getTextualRepresentation();

        // STEP 2: enriching of query and fetching data from the RuBQ API
        RuBQResult result = requestRuBQWebService(endpoint, questionString, lang);

        // STEP 3: add information to Qanary triplestore
        String sparql = getSparqlInsertQuery(myQanaryQuestion, result);
        myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint());

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

    protected RuBQResult requestRuBQWebService(URI uri, String questionString, String lang) throws URISyntaxException {
        RuBQRequest ruBQRequest = new RuBQRequest(uri, questionString, lang);

        HttpEntity<JSONObject> response = myRestTemplate.getForEntity(ruBQRequest.getRuBQQuestionUrlAsString(), JSONObject.class);

        return new RuBQResult(response.getBody(), ruBQRequest.getQuestion(), ruBQRequest.getRuBQEndpointUrl(), ruBQRequest.getLanguage());
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
    private String getSparqlInsertQuery(QanaryQuestion<String> myQanaryQuestion, RuBQResult result)
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