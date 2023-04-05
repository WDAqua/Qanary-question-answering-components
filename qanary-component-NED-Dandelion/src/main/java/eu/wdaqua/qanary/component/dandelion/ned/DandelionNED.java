package eu.wdaqua.qanary.component.dandelion.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.dandelion.ned.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.dandelion.ned.exception.ApiTokenIsNullOrEmptyException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties
 * (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class DandelionNED extends QanaryComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandelionNED.class);

    private final String applicationName;
    private final String apiToken;

    private RestTemplateWithCaching myRestTemplate;
    private CacheOfRestTemplateResponse myCacheOfResponses;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public DandelionNED(@Value("${spring.application.name}") final String applicationName, //
                        @Autowired RestTemplateWithCaching myRestTemplate, //
                        @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
                        @Value("${ned.dandelion.api.live.test.active}") final boolean apiLiveTestActive, //
                        @Value("${ned.dandelion.api.token}") final String apiToken //
    ) throws ApiTokenIsNullOrEmptyException, ApiLiveTestFaildException {
        if (apiToken == null || apiToken.isEmpty()) {
            throw new ApiTokenIsNullOrEmptyException();
        }

        this.applicationName = applicationName;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.apiToken = apiToken;

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

    private void testFunctionality() throws Exception {
        String myQuestion = "What is a test?";
        ArrayList<Link> links = new ArrayList<Link>();

        JsonObject jsonObject = this.sendRequestToDandelionAPI(myQuestion);

        if (jsonObject.has("annotations")) {
            JsonArray jsonArray = jsonObject.get("annotations").getAsJsonArray();
            if (jsonArray.size() != 0) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject explrObject = jsonArray.get(i).getAsJsonObject();
                    int begin = explrObject.get("start").getAsInt();
                    int end = explrObject.get("end").getAsInt();
                    String uri = explrObject.get("uri").getAsString();
                    String finalUri = "http://dbpedia.org/resource" + uri.substring(28);

                    Link l = new Link();
                    l.begin = begin;
                    l.end = end + 1;
                    l.link = finalUri;
                    links.add(l);
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

        // STEP 1: get the required data from the Qanary triplestore (the global process memory)
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();


        // STEP 2: enriching of query and fetching data from the Dandelion API
        LOGGER.debug("Question {}", myQuestion);

        JsonObject jsonObject = this.sendRequestToDandelionAPI(myQuestion);
        ArrayList<Link> links = this.getLinksFromAnnotation(jsonObject);


        // STEP 3: add information to Qanary triplestore
        this.updateTriplestore(links, myQanaryQuestion, myQanaryUtils);

        return myQanaryMessage;
    }

    public JsonObject sendRequestToDandelionAPI(String myQuestion) throws UnsupportedEncodingException, URISyntaxException {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append("https://api.dandelion.eu/datatxt/nex/v1/?text=");
        uriBuilder.append(URLEncoder.encode(myQuestion, "UTF-8"));
        uriBuilder.append("&include=types%2Cabstract%2Ccategories&token=");
        uriBuilder.append(apiToken);

        URI uri = new URI(uriBuilder.toString());

        long requestBefore = this.myCacheOfResponses.getNumberOfExecutedRequests();

        LOGGER.debug("URL: {}", uri);
        ResponseEntity<String> response = myRestTemplate.getForEntity(uri, String.class);

        //TODO: check if response is valid
        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", uri);
        } else {
            LOGGER.info("request was actually executed: {}", uri);
        }

        JsonObject jsonObject = JsonParser.parseString(response.getBody()).getAsJsonObject();
        LOGGER.debug("JSON: {}", jsonObject);

        return jsonObject;
    }

    public ArrayList<Link> getLinksFromAnnotation(JsonObject jsonObject) {
        ArrayList<Link> links = new ArrayList<Link>();

        if (jsonObject.has("annotations")) {
            JsonArray jsonArray = jsonObject.get("annotations").getAsJsonArray();
            if (jsonArray.size() != 0) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject explrObject = jsonArray.get(i).getAsJsonObject();
                    int begin = explrObject.get("start").getAsInt();
                    int end = explrObject.get("end").getAsInt();
                    LOGGER.debug("Begin: {}", begin);
                    LOGGER.debug("End: {}", end);
                    String uri = explrObject.get("uri").getAsString();
                    String finalUri = "http://dbpedia.org/resource" + uri.substring(28);
                    LOGGER.debug("Link {}", finalUri);

                    Link l = new Link();
                    l.begin = begin;
                    l.end = end + 1;
                    l.link = finalUri;
                    links.add(l);
                }
            }
        }

        return links;
    }

    public String getSparqlInsertQuery(Link link, QanaryQuestion<String> myQanaryQuestion) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(link.begin), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(link.end), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(link.link));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
        LOGGER.info("SPARQL query: {}", sparql);

        return sparql;
    }

    private void updateTriplestore( //
                                    ArrayList<Link> links, //
                                    QanaryQuestion<String> myQanaryQuestion, //
                                    QanaryUtils myQanaryUtils //
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        for (Link l : links) {
            String sparql = this.getSparqlInsertQuery(l, myQanaryQuestion);

            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
        }
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    class Link {
        public int begin;
        public int end;
        public String link;
    }
}
