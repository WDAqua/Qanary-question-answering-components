package eu.wdaqua.qanary.component.agdistis.ned;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.agdistis.ned.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.agdistis.ned.exception.ApiUrlIsNullOrEmptyException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 *
 * @author Dennis Diefenbach
 */

@Component
public class Agdistis extends QanaryComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Agdistis.class);

    private final String FILENAME_SPOTS_FROM_GRAPH = "/queries/select_all_spots_from_graph.rq";
    private final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    private final String applicationName;
    private final String apiUrl;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;

    public Agdistis(
            @Value("${spring.application.name}") final String applicationName, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Value("${agdistis.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${agdistis.api.url}") final String apiUrl //
    ) throws ApiUrlIsNullOrEmptyException, ApiLiveTestFaildException {
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new ApiUrlIsNullOrEmptyException();
        }

        this.applicationName = applicationName;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.apiUrl = apiUrl;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SPOTS_FROM_GRAPH);
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

    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        LOGGER.info("process: {}", myQanaryMessage);

        // STEP 1: get the required data from the Qanary triplestore (the global process memory)
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();


        // STEP 2: enriching of query and fetching data from the API
        LOGGER.info("Question {}", myQuestion);

        // Retrieves the spots from the knowledge graph
        String sparql = this.createSelectSparqlForSpots(myQanaryQuestion);
        ResultSet resultSet = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);
        ArrayList<Spot> spots = this.getSpotsFromResultSet(resultSet);

        // Information about the AGDISTIS API can be found here: https://github.com/AKSW/AGDISTIS/wiki/2-Asking-the-webservice
        // curl --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'" -d type='agdistis' http://139.18.2.164:8080/AGDISTIS
        // Match the format "The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>."

        JsonArray response = this.sendRequestToAPI(spots, myQuestion);
        List<Link> links = this.getLinksFromAnnotation(response);


        // STEP 3: add information to Qanary triplestore
        this.updateTriplestore(links, myQanaryQuestion, myQanaryUtils);


        return myQanaryMessage;
    }

    public String createSelectSparqlForSpots(QanaryQuestion<String> myQanaryQuestion) throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));

        return this.loadQueryFromFile(FILENAME_SPOTS_FROM_GRAPH, bindings);
    }

    public ArrayList<Spot> getSpotsFromResultSet(ResultSet r) {
        ArrayList<Spot> spots = new ArrayList<>();
        while (r.hasNext()) {
            QuerySolution s = r.next();
            Spot spot = new Spot();
            spot.begin = s.getLiteral("start").getInt();
            spot.end = s.getLiteral("end").getInt();

            LOGGER.info("Spot: {}-{}", spot.begin, spot.end);

            spots.add(spot);
        }

        return spots;
    }

    public MultiValueMap<String, String> createRequestBody(
            String myQuestion,
            List<Spot> spots
    ) throws UnsupportedEncodingException {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        String input = myQuestion;
        Integer offset = 0;
        for (Spot spot : spots) {
            input = input.substring(0, spot.begin + offset) + "<entity>"
                    + input.substring(spot.begin + offset, spot.end + offset)
                    + "</entity>"
                    + input.substring(spot.end + offset, input.length());
            offset += "<entity>".length() + "</entity>".length();
        }
        // String input="The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.";
        LOGGER.info("Input to AGDISTIS: {}", input);

        map.add("type", "agdistis");
        map.add("text", input);

        return map;
    }

    public JsonArray sendRequestToAPI(
            List<Spot> spots,
            String myQuestion
    ) throws UnsupportedEncodingException, URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = this.createRequestBody(myQuestion, spots);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        long requestBefore = this.myCacheOfResponses.getNumberOfExecutedRequests();
        ResponseEntity<String> response = this.myRestTemplate.postForEntity(new URI(this.apiUrl), request, String.class);

        //TODO: check if response is valid
        Assert.notNull(response);
        Assert.notNull(response.getBody());

        if (this.myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", this.apiUrl);
        } else {
            LOGGER.info("request was actually executed: {}", this.apiUrl);
        }

        LOGGER.info("JSON document from AGDISTIS API: {}", response.getBody());

        return JsonParser.parseString(response.getBody()).getAsJsonArray();
    }

    public List<Link> getLinksFromAnnotation(JsonArray response) {
        ArrayList<Link> links = new ArrayList<>();
        for (
                int i = 0; i < response.size(); i++) {
            if (response.get(i).getAsJsonObject().has("disambiguatedURL")) {
                Link l = new Link();
                l.link = response.get(i).getAsJsonObject().get("disambiguatedURL").getAsString();
                l.begin = response.get(i).getAsJsonObject().get("start").getAsInt() - 1;
                l.end = response.get(i).getAsJsonObject().get("start").getAsInt() - 1 + response.get(i).getAsJsonObject().get("offset").getAsInt();
                links.add(l);
            }
        }

        return links;
    }

    public String getSparqlInsertQuery(
            Link l, //
            QanaryQuestion<String> myQanaryQuestion //
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.begin), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.end), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(l.link));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
        LOGGER.info("SPARQL query: {}", sparql);

        return sparql;
    }

    private void updateTriplestore(
            List<Link> links, //
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

    private void testFunctionality() throws UnsupportedEncodingException, URISyntaxException, ApiLiveTestFaildException {
        String myQuestion = "What is the birthplace of <entity>Albert Einstein</entity>?";
        List<Spot> spots = new ArrayList<>();

        JsonArray json = this.sendRequestToAPI(spots, myQuestion);
        List<Link> selections = this.getLinksFromAnnotation(json);

        if (selections.isEmpty()) {
            throw new ApiLiveTestFaildException("No selections found");
        }
    }

    class Spot {
        public int begin;
        public int end;
    }

    class Link {
        public int begin;
        public int end;
        public String link;
    }
}
