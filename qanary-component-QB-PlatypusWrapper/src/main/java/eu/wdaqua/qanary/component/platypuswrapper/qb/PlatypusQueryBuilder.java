package eu.wdaqua.qanary.component.platypuswrapper.qb;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.DataNotProcessableException;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.PlatypusRequest;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.PlatypusResult;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import net.minidev.json.JSONObject;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
/**
 * This Qanary component fetches the SPARQL query for
 * the enriched question from the Platypus API
 *
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary-question-answering-components/blob/master/qanary-component-QB-TeBaQaWrapper/README.md"
 *      target="_top">README.md</a>
 */ public class PlatypusQueryBuilder extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(PlatypusQueryBuilder.class);
    private final float threshold;
    private final URI endpoint;
    private final RestTemplate myRestTemplate;
    private final String langDefault;
    private final List<String> supportedLang;
    private final String applicationName;
    private final CacheOfRestTemplateResponse myCacheOfResponses;
    private QanaryUtils myQanaryUtils;

    public PlatypusQueryBuilder(//
                                float threshold, //
                                @Qualifier("platypus.langDefault") String langDefault, //
                                @Qualifier("platypus.endpoint.language.supported") List<String> supportedLang, //
                                @Qualifier("platypus.endpointUrl") URI endpoint, //
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
                "langDefault is invalid (requires exactly 2 characters, e.g., 'en'), was " + langDefault + " (length=" + langDefault.length() + ")";
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

    public List<String> getSupportedLang() {
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
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        // retrieve language from Qanary triplestore via commons method
        String lang = null;
        try {
            lang = myQanaryQuestion.getLanguage();
            if (lang == null) {
                logger.warn("no language found. Using default ({})", langDefault);
                lang = langDefault;
            }
        } catch (Exception e) {
            logger.warn("no language annotation detected. Using default ({})", langDefault);
            lang = langDefault;
        }

        if (isLangSupported(lang) == false) {
            logger.warn("lang ({}) is not supported", lang);
            return myQanaryMessage;
        } 
        logger.debug("proceeding with lang: {}", lang);

        // STEP 1: get the required data from the Qanary triplestore (the global process
        // memory)
        String questionString = myQanaryQuestion.getTextualRepresentation();

        // STEP 2: enriching of query and fetching data from the Platypus API
        PlatypusResult result = requestPlatypusWebService(endpoint, questionString, lang);

        if (result == null) {
            logger.error("No result from Platypus API");
            return myQanaryMessage;
        }

        // STEP 3: add information to Qanary triplestore
        //String sparql = getSparqlInsertQuery(myQanaryQuestion, result);
        //myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
        int index = 0; // only one query expected
        QuerySolutionMap bindings = populateBindings(myQanaryQuestion, result, index);
        String insertAnswerSPARQL = getSparqlInsertQueryForAnswerSPARQL(bindings);
        String insertTypedLiteral = getSparqlInsertQueryForTypedLiteral(bindings);
        myQanaryUtils.getQanaryTripleStoreConnector().update(insertAnswerSPARQL);
        myQanaryUtils.getQanaryTripleStoreConnector().update(insertTypedLiteral);

        return myQanaryMessage;
    }

    protected boolean isLangSupported(String lang) {
        for (int i = 0; i < supportedLang.size(); i++) {
            if (supportedLang.get(i).equals(lang)) {
                return true;
            }
        }

        return false;
    }

    public PlatypusResult requestPlatypusWebService(@RequestBody PlatypusRequest request) throws URISyntaxException, DataNotProcessableException {
        return requestPlatypusWebService(
                request.getPlatypusEndpointUrl(),
                request.getQuestion(),
                request.getLanguage());
    }

    protected PlatypusResult requestPlatypusWebService(URI uri, String questionString, String lang) throws URISyntaxException, DataNotProcessableException {
        PlatypusRequest platypusRequest = new PlatypusRequest(uri, questionString, lang);
        long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

        logger.debug("URL: {}", platypusRequest.getPlatypusQuestionUrlAsString());
        HttpEntity<JSONObject> response = myRestTemplate.getForEntity(platypusRequest.getPlatypusQuestionUrlAsString(), JSONObject.class);

        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            logger.warn("request was cached: {}", platypusRequest);
        } else {
            logger.info("request was actually executed: {}", platypusRequest);
        }

        if (response.getBody().equals("{}")) {
            return null;
        } else {
            return new PlatypusResult(response.getBody(), platypusRequest.getQuestion(), platypusRequest.getPlatypusEndpointUrl(), platypusRequest.getLanguage());
        }
    }

    private String cleanStringForSparqlQuery(String myString) {
        return myString.replaceAll("\"", "\\\"").replaceAll("\n", "");
    }

    /**
     * TODO: update 
     * creates the SPARQL query for inserting the data into Qanary triplestore
     * <p>
     * the data can be retrieved via SPARQL 1.1 from the Qanary triplestore using
     * QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL from qanary.commons
     * which is providing a predefined query template, s.t., the created data is
     * conform with the expectations of other Qanary components
     *
     * @param myQanaryQuestion
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    protected String getSparqlInsertQueryForAnswerSPARQL(QuerySolutionMap bindings) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

        // get the template of the INSERT query for AnnotationOfAnswerSPARQL
        String sparql = QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL(bindings);

        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

        return sparql;
    }

    /**
     * TODO: update 
     * creates the SPARQL query for inserting the data into Qanary triplestore
     * <p>
     * the data can be retrieved via SPARQL 1.1 from the Qanary triplestore using
     * QanaryTripleStoreConnector.insertAnnotationOfTypedLiteral from qanary.commons
     * which is providing a predefined query template, s.t., the created data is
     * conform with the expectations of other Qanary components
     *
     * @param myQanaryQuestion
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    protected String getSparqlInsertQueryForTypedLiteral(QuerySolutionMap bindings) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

        // get the template of the INSERT query for AnnotationOfAnswerSPARQL
        String sparql = QanaryTripleStoreConnector.insertAnnotationOfTypedLiteral(bindings);

        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

        return sparql;
    }

    protected QuerySolutionMap populateBindings(QanaryQuestion<String> myQanaryQuestion, PlatypusResult result, int index) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {

        String answerSparql = cleanStringForSparqlQuery(result.getSparql());

        // define here the parameters for the SPARQL INSERT query
        QuerySolutionMap bindings = new QuerySolutionMap();
        // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindings.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createStringLiteral(answerSparql));
        bindings.add("confidence", ResourceFactory.createTypedLiteral(result.getConfidence()));
        bindings.add("index", ResourceFactory.createTypedLiteral(index));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
        // TODO: add result value with correct datatype
        // TODO: how does this handle a *list* of values? 
        // in its current implementation, result.getValues can only return a list of length 1
        // how should multiple results be handled
        // TODO: (add to desc)
        //  generate ALL bindings, then pass to differnet methods
        //  each method will only use the bindings it needs (determined by used query template)

        // the actual result 
        //bindings.add("answerValue", ResourceFactory.createPlainLiteral(result.getValues().get(0)));
        // TODO: perpexlingly, this does not equate to "value"xsd:string
        bindings.add("answerValue", ResourceFactory.createTypedLiteral(result.getValues().get(0)));
        // the result data type 
        bindings.add("answerDataType", ResourceFactory.createResource(result.getDatatype().toString()));

        logger.debug("created bindings: {}", bindings.toString());

        return bindings;
    }

    // TODO: refactor bindings to external method, then pass as param
    // TODO: create two `get*SparqlInsertQuery()` methods:
    //          one for AnswerSPARQL
    //          one for typed literal (Answer + AnswerType)
    // TODO: execute *both* queries in main process
}
