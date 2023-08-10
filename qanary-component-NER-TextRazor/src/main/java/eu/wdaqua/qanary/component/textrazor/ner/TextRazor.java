package eu.wdaqua.qanary.component.textrazor.ner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.textrazor.ner.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.textrazor.ner.exception.ApiTokenIsNullOrEmptyException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class TextRazor extends QanaryComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextRazor.class);

    private final String applicationName;
    private final String apiUrl;
    private final String apiKey;

    private RestTemplate myRestTemplate;
    private CacheOfRestTemplateResponse myCacheOfResponses;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";


    public TextRazor(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${textrazor.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${textrazor.api.url}") final String url, //
            @Value("${textrazor.api.key}") final String apiKey, //
            RestTemplate myRestTemplate, //
            CacheOfRestTemplateResponse myCacheOfResponses //
    ) throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ApiTokenIsNullOrEmptyException();
        }


        this.applicationName = applicationName;
        this.apiUrl = url;
        this.apiKey = apiKey;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);

        if (apiLiveTestActive) {
            LOGGER.info("API live testing is activated");

            for (int i = 0; i < 10; i++) {
                try {
                    this.testFunctionality();
                    LOGGER.info("Functionality works as expected");
                    break;
                } catch (Exception ex) {
                    LOGGER.warn("Functionality did not work as expected on attempt no. {}: {}", i, ex.toString());
                    if (i > 8) {
                        LOGGER.error("Functionality does not work as expected. Exiting..");
                        throw new ApiLiveTestFaildException("Could not start component, {}" + applicationName);
                    }
                }
            }
        }
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component
     *
     * @throws Exception
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        LOGGER.info("process: {}", myQanaryMessage);

        //STEP 1: Retrive the information needed for the question
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();


        // Step 2: Call the TextRazor service
        LOGGER.info("Question {}", myQuestion);

        JsonObject apiResponse = this.sendRequestToAPI(myQuestion);
        List<Selection> selections = this.getSelectionsFromAnnotation(apiResponse);


        // STEP 3: add information to Qanary triplestore
        this.updateTriplestore(selections, myQanaryQuestion, myQanaryUtils);


        return myQanaryMessage;
    }

    public JsonObject sendRequestToAPI(String myQuestion) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-textrazor-key", apiKey);
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("text", myQuestion);
        map.add("extractors", "entities");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        long requestBefore = this.myCacheOfResponses.getNumberOfExecutedRequests();

        LOGGER.debug("URL: {}", this.apiUrl);
        ResponseEntity<String> response = myRestTemplate.postForEntity(this.apiUrl, request, String.class);

        //TODO: check if response is valid
        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", this.apiUrl);
        } else {
            LOGGER.info("request was actually executed: {}", this.apiUrl);
        }

        JsonObject jsonObject = JsonParser.parseString(response.getBody()).getAsJsonObject();
        LOGGER.debug("JSON: {}", jsonObject);

        return jsonObject;
    }

    public List<Selection> getSelectionsFromAnnotation(JsonObject jsonObject) {
        ArrayList<Selection> selections = new ArrayList<>();

        if (jsonObject.has("response")) {
            JsonObject response = jsonObject.getAsJsonObject("response");

            if (response.has("entities")) {
                JsonArray ents = response.getAsJsonArray("entities");

                for (int i = 0; i < ents.size(); i++) {
                    JsonObject explrObject = ents.get(i).getAsJsonObject();
                    int begin = explrObject.get("startingPos").getAsInt();
                    int end = explrObject.get("endingPos").getAsInt();

                    LOGGER.debug("Question: {}", explrObject);
                    LOGGER.debug("Question: {}", begin);
                    LOGGER.debug("Question: {}", end);

                    Selection s = new Selection();
                    s.begin = begin;
                    s.end = end;
                    selections.add(s);
                }
            }
        }

        return selections;
    }

    public String getSparqlInsertQuery(
            Selection selection, //
            QanaryQuestion<String> myQanaryQuestion //
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(selection.begin), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(selection.end), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
        LOGGER.info("SPARQL query: {}", sparql);

        return sparql;
    }

    private void updateTriplestore(
            List<Selection> selections, //
            QanaryQuestion<String> myQanaryQuestion, //
            QanaryUtils myQanaryUtils //
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        for (Selection s : selections) {
            String sparql = this.getSparqlInsertQuery(s, myQanaryQuestion);

            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
        }
    }

    private void testFunctionality() throws ApiLiveTestFaildException {
        String myQuestion = "What is the birthplace of Albert Einstein?";

        JsonObject jsonObject = this.sendRequestToAPI(myQuestion);
        List<Selection> selections = this.getSelectionsFromAnnotation(jsonObject);

        if (selections.isEmpty()) {
            throw new ApiLiveTestFaildException("No selections found");
        }
    }

    class Selection {
        public int begin;
        public int end;
    }
}
