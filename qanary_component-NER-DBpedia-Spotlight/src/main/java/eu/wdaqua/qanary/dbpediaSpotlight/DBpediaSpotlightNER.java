package eu.wdaqua.qanary.dbpediaSpotlight;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jena.vocabulary.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * represents a wrapper of the DBpedia Spotlight tool used here as a spotter
 *
 * @author Kuldeep Singh
 */

@Component
public class DBpediaSpotlightNER extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNER.class);

    private final String applicationName;

    public DBpediaSpotlightNER(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;
    }
    /**
     * default processor of a QanaryMessage
     */

    private List<String> usingXml(String urladd) {
        List<String> retLst = new ArrayList<String>();
        try {

            URL url = new URL(urladd);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = null;
            connection = (HttpURLConnection) urlConnection;

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(connection.getInputStream());

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("surfaceForm");

            boolean flg = true;
            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;
                    String text = eElement.getAttribute("name");
                    String offset = eElement.getAttribute("offset");

                    String startEnd = Integer.parseInt(offset) + "," + (text.length() + Integer.parseInt(offset));
                    retLst.add(startEnd);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retLst;
    }

    private List<String> getResults(String input) {
        /*
		 * This can be an alternative for passing text using API String
		 * SpotterService = "https://api.dbpedia-spotlight.org/en/spot?text=";
		 * UriComponentsBuilder service =
		 * UriComponentsBuilder.fromHttpUrl(SpotterService) .queryParam("text",
		 * qns); logger.info("Service request "+service); RestTemplate
		 * restTemplate = new RestTemplate(); ResponseEntity<String> response =
		 * restTemplate.exchange(service.build().encode().toUri(),
		 * HttpMethod.GET, null, String.class); logger.info(
		 * "Xml document from alchemy api {}", response.getBody());
		 */

        // TODO: Should move to the config
        //more information about API available at: https://www.dbpedia-spotlight.org/api
        String madeUrlFromInput = "https://api.dbpedia-spotlight.org/en/spot?text=";
		/*
		 * String qns[] = input.split(" "); String append = String.join("%20",
		 * qns);
		 */
        try {
            logger.info("Input is: {}", input);
            madeUrlFromInput += URLEncoder.encode(input, "UTF-8");
            // +"&executeSparqlQuery=on&relationExtractorType=Semantic";
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn(e.getMessage());
        }

        List<String> retLst = new ArrayList<String>();
        {
            logger.info("URL is: {}", madeUrlFromInput);
            retLst = usingXml(madeUrlFromInput);
        }

        return retLst;
    }

    public QanaryMessage process(QanaryMessage myQanaryMessage) {
        long startTime = System.currentTimeMillis();
        logger.info("Qanary Message: {}", myQanaryMessage);

        try {

            // STEP1: Retrieve the named graph and the endpoint

            // the class QanaryUtils provides some helpers for standard tasks
            QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
            QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

            // question string is required as input for the service call
            String myQuestion = myQanaryQuestion.getTextualRepresentation();
            logger.info("Question: {}", myQuestion);

            // String uriQuestion="http://wdaqua.eu/dummy";
            // String question="Brooklyn Bridge was designed by Alfred";

            // STEP3: Pass the information to the component and execute it
            // logger.info("apply commons alignment on outgraph");

            DBpediaSpotlightNER qaw = new DBpediaSpotlightNER(applicationName);

            List<String> stEn = new ArrayList<String>();
            stEn = qaw.getResults(myQuestion);
            int cnt = 0;
            ArrayList<Selection> selections = new ArrayList<Selection>();
            for (String str : stEn) {
                Selection s1 = new Selection();
                String str1[] = str.split(",");
                s1.begin = Integer.parseInt(str1[0]);
                s1.end = Integer.parseInt(str1[1]);
                selections.add(s1);
            }

            //STEP4: Push the result of the component to the triplestore

            for (Selection s : selections) {
                String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> " //
                        + "prefix oa: <http://www.w3.org/ns/openannotation/core/> " //
                        + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " //
                        + "INSERT { " //
                        + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
                        + "  ?a a qa:AnnotationOfSpotInstance . " //
                        + "  ?a oa:hasTarget [ " //
                        + "           a    oa:SpecificResource; " //
                        + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
                        + "           oa:hasSelector  [ " //
                        + "                    a oa:TextPositionSelector ; " //
                        + "                    oa:start \"" + s.begin + "\"^^xsd:nonNegativeInteger ; " //
                        + "                    oa:end  \"" + s.end + "\"^^xsd:nonNegativeInteger  " //
                        + "           ] " //
                        + "  ] ; " //
                        + "     oa:annotatedBy <urn:qanary:"+this.applicationName+"> ; " //
                        + "	    oa:annotatedAt ?time  " //
                        + "}} " //
                        + "WHERE { " //
                        + "BIND (IRI(str(RAND())) AS ?a) ." //
                        + "BIND (now() as ?time) " //
                        + "}";
                myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            logger.info("Time {}", estimatedTime);

        } catch (Exception e) {// MalformedURLException e) {
            e.printStackTrace();
        }

        return myQanaryMessage;
    }

    class Selection {
        public int begin;
        public int end;
    }

}
