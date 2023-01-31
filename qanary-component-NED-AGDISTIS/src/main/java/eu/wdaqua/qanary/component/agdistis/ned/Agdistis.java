package eu.wdaqua.qanary.component.agdistis.ned;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 *
 * @author Dennis Diefenbach
 */

@Component
public class Agdistis extends QanaryComponent {
    @Value("${agdistis.service.url}")
    private String agdistisServiceUrl;
    private static final Logger logger = LoggerFactory.getLogger(Agdistis.class);

    private final String applicationName;
    private final String FILENAME_SPOTS_FROM_GRAPH = "/queries/select_all_spots_from_graph.rq";
    private final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public Agdistis(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SPOTS_FROM_GRAPH);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
    }

    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        try {
            long startTime = System.currentTimeMillis();
            logger.info("process: {}", myQanaryMessage);
            //STEP 1: Retrive the information needed for the question

            // the class QanaryUtils provides some helpers for standard tasks
            QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
            QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

            // Retrives the question string
            String myQuestion = myQanaryQuestion.getTextualRepresentation();

            // Retrieves the spots from the knowledge graph
            QuerySolutionMap bindings = new QuerySolutionMap();
            bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            String sparql = this.loadQueryFromFile(FILENAME_SPOTS_FROM_GRAPH, bindings);

            ResultSet r = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);

            ArrayList<Spot> spots = new ArrayList<Spot>();
            while (r.hasNext()) {
                QuerySolution s = r.next();
                Spot spot = new Spot();
                spot.begin = s.getLiteral("start").getInt();
                spot.end = s.getLiteral("end").getInt();
                logger.info("Spot: {}-{}", spot.begin, spot.end);
                spots.add(spot);
            }

            // Step 2: Call the AGDISTIS service
            // Information about the AGDISTIS API can be found here: https://github.com/AKSW/AGDISTIS/wiki/2-Asking-the-webservice
            // curl --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'" -d type='agdistis' http://139.18.2.164:8080/AGDISTIS
            // Match the format "The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>."
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
            logger.info("Input to Agdistis: " + input);
            UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(agdistisServiceUrl);
            logger.info("Service request " + service);
            String body = "type=agdistis&" + "text='" + URLEncoder.encode(input, "UTF-8") + "'";
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(service.build().encode().toUri(), body, String.class);
            logger.info("JSON document from Agdistis api {}", response);
            // Extract entities
            ArrayList<Link> links = new ArrayList<Link>();
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

            //STEP4: Push the result of the component to the triplestore
            logger.info("Apply commons alignment on outgraph");
            for (Link l : links) {
                QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
                bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
                bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
                bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.begin), XSDDatatype.XSDnonNegativeInteger));
                bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.end), XSDDatatype.XSDnonNegativeInteger));
                bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(l.link));
                bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

                // get the template of the INSERT query
                sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
                logger.info("Sparql query {}", sparql);
                myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            logger.info("Time {}", estimatedTime);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return myQanaryMessage;
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
