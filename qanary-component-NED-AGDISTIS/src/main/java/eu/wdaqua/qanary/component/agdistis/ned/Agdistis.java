package eu.wdaqua.qanary.component.agdistis.ned;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.agdistis.ned.exception.ApiUrlIsNullOrEmptyException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.shiro.util.Assert;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 *
 * @author Dennis Diefenbach
 */

@Component
public class Agdistis extends QanaryComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Agdistis.class);

    private final String applicationName;
    private final String apiUrl;

    private RestTemplateWithCaching myRestTemplate;
    private CacheOfRestTemplateResponse myCacheOfResponses;
    private final String FILENAME_SPOTS_FROM_GRAPH = "/queries/select_all_spots_from_graph.rq";
    private final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public Agdistis(
            @Value("${spring.application.name}") final String applicationName, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses, //
            @Value("${ned.agdistis.api.url}") final String apiUrl //
    ) throws ApiUrlIsNullOrEmptyException {
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new ApiUrlIsNullOrEmptyException();
        }

        this.applicationName = applicationName;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
        this.apiUrl = apiUrl;

        // TODO API LIVE TEST ?

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SPOTS_FROM_GRAPH);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
    }

    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        try {
            long startTime = System.currentTimeMillis();
            LOGGER.info("process: {}", myQanaryMessage);

            //STEP 1: Retrive the information needed for the question
            QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
            QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
            String myQuestion = myQanaryQuestion.getTextualRepresentation();

            // Retrieves the spots from the knowledge graph
            String sparql = this.createSelectSparqlForSpots(myQanaryQuestion);
            ResultSet resultSet = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);
            ArrayList<Spot> spots = this.getSpotsFromResultSet(resultSet);


            // Step 2: Call the AGDISTIS service
            // Information about the AGDISTIS API can be found here: https://github.com/AKSW/AGDISTIS/wiki/2-Asking-the-webservice
            // curl --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'" -d type='agdistis' http://139.18.2.164:8080/AGDISTIS
            // Match the format "The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>."
            String body = this.createRequestBody(myQuestion, spots);
            UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(this.apiUrl);
            LOGGER.info("Service request: {}", service);

            ArrayList<Link> links = this.sendRequestToAPI(service.build().encode().toUri(), body);

            // STEP 3: add information to Qanary triplestore
            for (Link l : links) {
                sparql = this.createInsertSparql(myQanaryQuestion, l);
                LOGGER.info("SPARQL query: {}", sparql);

                myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            LOGGER.info("Time: {}", estimatedTime);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return myQanaryMessage;
    }

    public String createSelectSparqlForSpots(QanaryQuestion<String> myQanaryQuestion) throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));

        return this.loadQueryFromFile(FILENAME_SPOTS_FROM_GRAPH, bindings);
    }

    public String createInsertSparql(
            QanaryQuestion<String> myQanaryQuestion, Link l
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.begin), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.end), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(l.link));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        return this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
    }

    public ArrayList<Spot> getSpotsFromResultSet( ResultSet r) {
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

    public String createRequestBody(String myQuestion, ArrayList<Spot> spots) throws UnsupportedEncodingException {
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

        return "type=agdistis&" + "text='" + URLEncoder.encode(input, "UTF-8") + "'";
    }

    public ArrayList<Link> sendRequestToAPI(URI apiUri, String body) {
        long requestBefore = this.myCacheOfResponses.getNumberOfExecutedRequests();

        String response = this.myRestTemplate.postForObject(apiUri, body, String.class);

        //TODO: check if response is valid
        Assert.notNull(response);

        if (this.myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
            LOGGER.warn("request was cached: {}", apiUri);
        } else {
            LOGGER.info("request was actually executed: {}", apiUri);
        }

        LOGGER.info("JSON document from AGDISTIS API: {}", response);

        // Extract entities
        ArrayList<Link> links = new ArrayList<>();
        JSONArray arr = new JSONArray(response);
        for (int i = 0; i < arr.length(); i++) {
            if (!arr.getJSONObject(i).isNull("disambiguatedURL")) {
                Link l = new Link();
                l.link = arr.getJSONObject(i).getString("disambiguatedURL");
                l.begin = arr.getJSONObject(i).getInt("start") - 1;
                l.end = arr.getJSONObject(i).getInt("start") - 1 + arr.getJSONObject(i).getInt("offset");
                links.add(l);
            }
        }

        return links;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
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
