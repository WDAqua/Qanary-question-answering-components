package eu.wdaqua.qanary.component.diambiguationclass.ned;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.http.client.ClientProtocolException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
// TODO: remove try catch blocks with empty catch blocks
public class DiambiguationClass extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(DiambiguationClass.class);

    @Value("${agdistis.url}")
    private String agdistisUrl;

    @Value("${templategeneration.url}")
    private String templategenerationUrl;

    private final String applicationName;
    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";
    private String FILENAME_SELECT_ANNOTATED_LANGUAGES = "/queries/select_all_AnnotationOfQuestionLanguage.rq";

    public DiambiguationClass(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_ANNOTATED_LANGUAGES);
    }

    public static String runCurlGetWithParam(String weburl, String data, String contentType)
            throws ClientProtocolException, IOException {
        String xmlResp = "";

        URL url = new URL(weburl + "data=" + URLEncoder.encode(data, "UTF-8"));
        //URL url = new URL(weburl+"?data="+URLEncoder.encode(data,"UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        //connection.setDoOutput(true);
        //connection.setRequestProperty("data", data);
        connection.setRequestProperty("Content-Type", contentType);
				
		/*DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(data);
		wr.flush();
		wr.close();*/


        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        xmlResp = response.toString();
        logger.debug("the curl GET output: {}", xmlResp);
        return xmlResp;
    }

    /**
     * runCurlPOSTWithParam is a function to fetch the response from a CURL command
     * using POST.
     */
    public static String runCurlPOSTWithParam(String weburl, String data, String contentType) throws Exception {

        // The String xmlResp is to store the output of the Template generator web
        // service accessed via CURL command

        String xmlResp = "";
        try {
            URL url = new URL(weburl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            connection.setRequestProperty("Content-Type", contentType);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            xmlResp = response.toString();

            logger.info("Response: {}", xmlResp);
        } catch (Exception e) {
        }
        return (xmlResp);

    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        //org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("process: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();

        // STEP2: Retrieve information that are needed for the computations
        // Here, we need two parameters as input to be fetched from triplestore-
        // question and language of the question.
        logger.info("Question: {}", myQuestion);

        // the below-mentioned SPARQL query to fetch annotation of language from
        // triplestore

        QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
        bindingsForSelect.add("graph", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));

        // get the template of the INSERT query
        String sparql = this.loadQueryFromFile(FILENAME_SELECT_ANNOTATED_LANGUAGES, bindingsForSelect);
        logger.info("SPARQL query: {}", sparql);
        ResultSet result1 = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);

        // Now fetch the language, in our case it is "en".

        String language1 = "en";
        logger.info("Language of the question: {}", language1);

        String data = "";
        String contentType = "application/json";

        // http://repository.okbqa.org/components/21 is the template generator URL
        // Sample input for this is mentioned below.
        /*
         * { "string": "Which river flows through Seoul?", "language": "en" }
         * http://ws.okbqa.org:1515/templategeneration/rocknrole
         */

        // now arrange the Web service and input parameters in the way, which is needed
        // for CURL command

        data = "{  \"string\":\"" + myQuestion + "\",\"language\":\"" + language1 + "\"}";// "{ \"string\": \"Which river
        // flows through Seoul?\",
        // \"language\": \"en\"}";
        logger.debug("data : {}", data);
        logger.debug("OKBQA component: 21");
        String output1 = "";
        // pass the input in CURL command and call the function.

        try {
            output1 = DiambiguationClass.runCurlPOSTWithParam(templategenerationUrl, data, contentType);
        } catch (Exception e) {
            logger.error("Error in runCurlPOSTWithParam", e);
        }
        // System.out.println("The output template is:" +output1);
        logger.info("The output template is: {}", output1);
        /*
         * once output is recieved, now the task is to parse the generated template, and
         * store the needed information which is Resource, Property, Resource Literal,
         * and Class. Then push this information back to triplestore The below code
         * before step 4 does parse the template. For parsing PropertyRetrival.java file
         * is used, which is in the same package.
         *
         * for this, we create a new object property of Property class.
         *
         */

        data = output1.substring(1, output1.length() - 1);

        contentType = "application/json";

        logger.debug("data: {}", data);
        logger.debug("OKBQA component: 7");
        output1 = "";
        try {
            logger.debug("Calling inside ===============");
            output1 = DiambiguationClass.runCurlGetWithParam(agdistisUrl, data, contentType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("The output template is: {}", output1);

        Map<String, Map<String, Double>> allClassses = new HashMap<String, Map<String, Double>>();
        try {

            JSONObject json = new JSONObject(output1);

            JSONArray characters = (JSONArray) json.get("ned");
            Iterator i = characters.iterator();

            while (i.hasNext()) {
                JSONObject mainObject = (JSONObject) i.next();
                JSONArray types = (JSONArray) mainObject.get("classes");
                Iterator iTypes = types.iterator();
                String var = "";

                while (iTypes.hasNext()) {
                    Map<String, Double> urlsAndScore = new HashMap<String, Double>();
                    JSONObject tempObject = (JSONObject) iTypes.next();
                    String urls = (String) tempObject.get("value");
                    double score = (double) tempObject.get("score");
                    var = (String) tempObject.get("var");
                    if (allClassses.size() > 0 && allClassses.containsKey(var)) {

                        double tScore = (double) allClassses.get(var).entrySet().iterator().next().getValue();
                        if (tScore < score) {

                            allClassses.get(var).remove(allClassses.get(var).entrySet().iterator().next().getKey());
                            allClassses.get(var).put(urls, score);
                            logger.debug("var: {}, url: {}, score: {}", var, urls, score);
                        }

                    } else {
                        urlsAndScore.put(urls, score);
                        logger.debug("var: {}, url: {} , score: {}", var, urls, score);
                        // allUrls.put("classes", urlsAndScore);
                        allClassses.put(var, urlsAndScore);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String vars : allClassses.keySet()) {
            Map<String, Double> allUrls = allClassses.get(vars);
            int count = 0;
            for (String urls : allUrls.keySet()) {
                logger.debug("Inside : Literal: {}", urls);

                QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
                bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
                bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
                bindingsForInsert.add("answer", ResourceFactory.createResource(urls));
                bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

                // get the template of the INSERT query
                sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
                logger.info("SPARQL query: {}", sparql);
                myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

                count++;
            }
            logger.debug("Count is: {}", count);
        }
        return myQanaryMessage;

    }

    @Deprecated(since="3.1.3", forRemoval=true)
    private void loadTripleStore(String sparqlQuery, String endpoint) {
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
    }

    @Deprecated(since="3.1.3", forRemoval=true)
    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    class ClassesStructure {
        String var = "";
        String value = "";
        double score = 0.0;
    }
}
