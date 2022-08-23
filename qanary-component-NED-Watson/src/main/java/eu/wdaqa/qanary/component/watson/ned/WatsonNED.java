package eu.wdaqa.qanary.component.watson.ned;

import com.google.gson.Gson;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;


@Component
/**
 * This component retrieves named entities for a given question from the
 * IBM Watson Natural Language Understanding Web Service
 */
public class WatsonNED extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(WatsonNED.class);

    private final String applicationName;
    private final boolean cacheEnabled;
    private final String cacheFile;
    private final URI watsonServiceURL;
    private final String watsonServiceKey;

    public WatsonNED(
            @Value("${spring.application.name}") final String applicationName,
            @Value("${ned-watson.cache.enabled}") final boolean cacheEnabled,
            @Value("${ned-watson.cache.file}") final String cacheFile,
            @Value("${ned-watson.service.url}") final URI watsonServiceURL,
            @Value("${ned-watson.service.key}") final String watsonServiceKey
    ) {
        this.applicationName = applicationName;
        this.cacheEnabled = cacheEnabled;
        this.cacheFile = cacheFile;
        this.watsonServiceURL = watsonServiceURL;
        this.watsonServiceKey = watsonServiceKey;
    }

    /**
     * method encapsulating the functionality of the Qanary component
     *
     * @throws SparqlQueryFailed
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);

        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        // textual representation/String of question
        String myQuestionText = myQanaryQuestion.getTextualRepresentation();

        // variable setup
        List<NamedEntity> namedEntityList = new ArrayList<>();
        boolean hasCacheResult = false;

        // if cache is enabled in application.properties, try to find the question in the text file
        // add all found entities to the List
        if (cacheEnabled) {
            CacheResult cacheResult = readFromCache(myQuestionText);
            hasCacheResult = cacheResult.hasCacheResult;
            namedEntityList.addAll(cacheResult.dataWatson);
        }

        // if no cacheResult was found, or if cache is turned off, get data from Watson service and add it to the List
        if (!hasCacheResult) {
            namedEntityList = this.retrieveDataFromWebService(myQuestionText);
        }

        for (NamedEntity namedEntity : namedEntityList) {
            // the SPARQL query to push all entities into the triplestore
            String sparqlUpdateQuery = "" //
                    		+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                            + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>  " //
                            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" //
                            + "INSERT { " //
                            + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
                            + "?a a qa:AnnotationOfInstance . " //
                            + "?a oa:hasTarget [ " //
                            + "	a oa:SpecificResource; " //
                            + "	oa:hasSource    <" + myQanaryQuestion.getUri() + ">; \n" //
                            + "	oa:hasSelector  [ " //
                            + "		a oa:TextPositionSelector ; " //
                            + "		oa:start \"" + namedEntity.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
                            + "		oa:end  \"" + namedEntity.getEnd() + "\"^^xsd:nonNegativeInteger ; " //
                            + "		qa:score \"" + namedEntity.getConfidence() + "\"^^xsd:float " //
                            + "	] " //
                            + "] . " //
                            + "?a oa:hasBody <" + namedEntity.getUri() + "> ; \n" //
                            + "oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; " //
                            + "oa:annotatedAt ?time  " //
                            + "}} " //
                            + "WHERE { " //
                            + "BIND (IRI(str(RAND())) AS ?a) . " //
                            + "BIND (now() as ?time) " //
                            + "} ";

            myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlUpdateQuery);
        }
        return myQanaryMessage;
    }

    /**
     * Requests Data from the Watson WebService
     *
     * @param myQuestionText The question as String
     * @return a List with all found Named Entities, which will be further processed in "process"
     * @throws IOException
     */
    protected List<NamedEntity> retrieveDataFromWebService(String myQuestionText) throws IOException {
        logger.info("Retrieving data from Webservice for Question: {}", myQuestionText);
        ArrayList<NamedEntity> namedEntityArrayList = new ArrayList<>();

        // language of the question
        String questionLang = "en";

        /**
         * the request body as a JSONObject
         * "features" defines what the API will return
         * "entities" returns the entities with a dbpedia-link if it can find one
         * "mentions" so the location of the entity is returned
         * standard limit for entities is 50
         *
         * JSONObject should look like this:
         * {
         *      "language": "en",
         *      "text": "questionTextHere",
         *      "features": {
         *        "entities": {
         *          "limit": 5,
         *          "mentions": true
         *        }
         *      }
         * }
         */
        // create request body as JSONObject
        JSONObject requestBody = new JSONObject();
        requestBody.put("language", questionLang);
        requestBody.put("text", myQuestionText);

        // create request features for entities
        JSONObject requestFeaturesEntities = new JSONObject();
        requestFeaturesEntities.put("limit", 5);
        requestFeaturesEntities.put("mentions", true);
        // add request features
        JSONObject requestFeatures = new JSONObject();
        requestFeatures.put("entities", requestFeaturesEntities);
        requestBody.put("features", requestFeatures);

        // encodes the API key for Authorization
        String encodedKey = Base64.getEncoder().encodeToString(("apikey:" + watsonServiceKey).getBytes());

        // instances the http headers and sets them
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "Qanary/" + this.getClass().getName());

        // creates the Spring requestEntity, http entity seems to be unable to handle JSONObjects
        HttpEntity<String> requestEntity = new HttpEntity<String>(requestBody.toString(), headers);

        // creates the Spring RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        /**
         * executes the http request, needs
         * watsonServiceURL of type URI, the HTTPMethod, the Spring requestEntity and the class of the response type
         * seemingly can't directly return JSON
         */
        ResponseEntity<String> response = restTemplate.exchange(watsonServiceURL, HttpMethod.POST, requestEntity, String.class);

        try {
            String returnedStringResponse = response.getBody();
            if (returnedStringResponse != null) {
                JSONObject responseJsonObject = new JSONObject(returnedStringResponse);
                // test if response returned the entities Array
                if (responseJsonObject.has("entities")) {
                    // get the entity Array
                    JSONArray responseEntitiesArray = (JSONArray) responseJsonObject.get("entities");
                    // test if Array holds entities
                    if (responseEntitiesArray.length() != 0) {
                        // process each returned entity in the Array
                        for (int i = 0; i < responseEntitiesArray.length(); i++) {
                            // get the entity and log it
                            JSONObject responseEntity = responseEntitiesArray.getJSONObject(i);
                            logger.info("responseEntity: {}", responseEntity);

                            // check if entity has a disambiguation Array that contains the DBpedia URI
                            if (responseEntity.has("disambiguation")) {
                                // get location in the question of the entity
                                JSONArray locationsArray = (JSONArray) responseEntity.getJSONArray("mentions").getJSONObject(0).get("location");
                                int begin = locationsArray.getInt(0);
                                int end = locationsArray.getInt(1) - 1;

                                // get the confidence assigned by Watson
                                double confidence = (double) responseEntity.get("confidence");

                                // get the disambiguated DBpedia URI
                                String uri = (String) responseEntity.getJSONObject("disambiguation").get("dbpedia_resource");
                                logger.info("dbpedia_resource: {}, begin: {}, end: {}, confidence: {}", uri, begin, end, confidence);

                                // create new NamedEntity with all Data and add it to the ArrayList
                                NamedEntity foundNamedEntity = new NamedEntity(uri, begin, end, confidence);
                                namedEntityArrayList.add(foundNamedEntity);
                            }
                        }
                    }
                }
            }
            if (cacheEnabled) {
                this.writeToCache(myQuestionText, namedEntityArrayList);
            }
        } catch (JSONException e) {
            // handle this
            logger.error("JSONException: {}", e);
        }
        return namedEntityArrayList;
    }

    /**
     * Searches a text file and tries to find the asked question
     * if the question is in the file, return the answer and add it to the Array in CacheResult
     *
     * @param myQuestionText The String of the asked question
     * @return the CacheResult with all found answers
     * @throws IOException
     */
    private CacheResult readFromCache(String myQuestionText) throws IOException {
        final CacheResult cacheResult = new CacheResult();
        try {
            File f = new File(this.cacheFile);
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;

            while ((line = br.readLine()) != null) {
                String question = line.substring(0, line.indexOf("Answer:"));

                if (question.trim().equals(myQuestionText)) {
                    String answer = line.substring(line.indexOf("Answer:") + "Answer:".length());
                    logger.info("Here: {}", answer);
                    answer = answer.trim();
                    JSONArray jsonArr = new JSONArray(answer);
                    if (jsonArr.length() != 0) {
                        for (int i = 0; i < jsonArr.length(); i++) {
                            JSONObject explrObject = jsonArr.getJSONObject(i);
                            NamedEntity namedEntity = new NamedEntity((String) explrObject.get("uri"), (int) explrObject.get("begin"), (int) explrObject.get("end"), (double) explrObject.get("confidence"));
                            cacheResult.dataWatson.add(namedEntity);
                        }
                    }
                    cacheResult.hasCacheResult = true;
                    break;
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            // handle this
            logger.error("File not found: \n{}", e);
        }
        return cacheResult;
    }

    /**
     * Used to write questions and their answers into a text file as a simple cache
     *
     * @param myQuestionText The String of the asked question
     * @param uriAndLocation The Array of all named entities
     * @throws IOException
     */
    private void writeToCache(String myQuestionText, ArrayList<NamedEntity> uriAndLocation) throws IOException {
        try {
            // set true in FileWriter constructor, to append everything at the end of the cache file
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(cacheFile, true));
            Gson gson = new Gson();

            String json = gson.toJson(uriAndLocation);
            logger.info("gsonwala: {}", json);

            String mainString = myQuestionText + " Answer: " + json;
            bufferedWriter.append(mainString);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (FileNotFoundException e) {
            // handle this
            logger.error("File not found: \n{}", e);
        }
    }

    /**
     * Class used to return CacheResult
     */
    class CacheResult {
        public ArrayList<NamedEntity> dataWatson = new ArrayList<>();
        public boolean hasCacheResult;
    }
}
