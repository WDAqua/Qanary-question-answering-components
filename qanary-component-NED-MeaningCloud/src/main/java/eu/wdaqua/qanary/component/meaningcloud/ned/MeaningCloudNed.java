package eu.wdaqua.qanary.component.meaningcloud.ned;

import com.google.gson.Gson;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class MeaningCloudNed extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(MeaningCloudNed.class);

    private final String applicationName;
    private final String cacheFilePath;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    private String meaningCloudUrl;
    private String meaningCloudKey;

    public MeaningCloudNed(@Value("${spring.application.name}") final String applicationName,
                           @Value("${ned-meaningcloud.cache.file}") final String cacheFilePath,
                           @Value("${meaningcloud.api.url}") final String meaningCloudUrl,
                           @Value("${meaningcloud.api.key}") final String meaningCloudKey
    ) throws Exception {
        this.applicationName = applicationName;
        this.cacheFilePath = cacheFilePath;
        this.meaningCloudUrl = meaningCloudUrl;
        this.meaningCloudKey = meaningCloudKey;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);

        for (int i = 0; i < 10; i++) {
            try {
                this.testFunctionality();
                logger.info("Functionality works as expected");
                break;
            } catch (Exception ex) {
                logger.warn("Functionality did not work as expected on attempt no. {}: {}", i, ex.toString());
                if (i > 8) {
                    logger.error("Functionality does not work as expected. Exiting..");
                    throw new Exception("Could not start component, " + applicationName);
                }
            }
        }
    }

    private void testFunctionality() throws Exception {
        String myQuestion = "Is Selwyn Lloyd the prime minister of Winston Churchill?";
        ArrayList<Selection> selections = new ArrayList<Selection>();

        String thePath = "";
        thePath = URLEncoder.encode(myQuestion, "UTF-8");

        HttpClient httpclient = HttpClients.createDefault();
        String url = meaningCloudUrl + "?key=" + meaningCloudKey + "&of=json&lang=en&ilang=en&txt="+ thePath + "&tt=e&uw=y";
        HttpGet httpget = new HttpGet(url);
        // httpget.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpclient.execute(httpget);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = entity.getContent();
            String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
            JSONObject response2 = new JSONObject(text);
            if (response2.has("entity_list")) {
                JSONArray ents = (JSONArray) response2.get("entity_list");
                for (int j = 0; j < ents.length(); j++) {
                    JSONObject formObject = (JSONObject) ents.getJSONObject(j);
                    if (formObject.has("variant_list")) {
                        JSONArray jsonArray = (JSONArray) formObject.get("variant_list");
                        String link = null;
                        if (formObject.has("semld_list")) {
                            JSONArray jsonArray_semld_list = (JSONArray) formObject.get("semld_list");
                            link = jsonArray_semld_list.getString(0);
                        }
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject explrObject = jsonArray.getJSONObject(i);
                            int begin = explrObject.getInt("inip");
                            int end = explrObject.getInt("endp");
                            Selection s = new Selection();
                            s.begin = begin;
                            s.end = end;
                            String finalUrl = "";
                            if (link != null && link.contains("wikipedia")) {
                                finalUrl = "http://dbpedia.org/resource" + link.substring(28);
                            }
                            s.link = finalUrl;
                            selections.add(s);
                        }
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
        logger.info("process: {}", myQanaryMessage);
        // TODO: implement processing of question

        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        //String myQuestion = "Is Selwyn Lloyd the prime minister of Winston Churchill ?";
        ArrayList<Selection> selections = new ArrayList<Selection>();
        logger.info("Question {}", myQuestion);
        try {
            File f = new File(cacheFilePath);
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            int flag = 0;
            String line;
//				Object obj = parser.parse(new FileReader("DandelionNER.json"));
//				JSONObject jsonObject = (JSONObject) obj;
//				Iterator<?> keys = jsonObject.keys();

            while ((line = br.readLine()) != null && flag == 0) {
                String question = line.substring(0, line.indexOf("Answer:"));
                logger.info("{}", line);
                logger.info("{}", myQuestion);

                if (question.trim().equals(myQuestion)) {
                    String Answer = line.substring(line.indexOf("Answer:") + "Answer:".length());
                    logger.info("Here {}", Answer);
                    Answer = Answer.trim();
                    JSONArray jsonArr = new JSONArray(Answer);
                    if (jsonArr.length() != 0) {
                        for (int i = 0; i < jsonArr.length(); i++) {
                            JSONObject explrObject = jsonArr.getJSONObject(i);

                            logger.info("Question: {}", explrObject);

                            Selection l = new Selection();
                            l.begin = (int) explrObject.get("begin");
                            l.end = (int) explrObject.get("end");
                            l.link = explrObject.getString("link");
                            selections.add(l);
                        }
                    }
                    flag = 1;

                    break;
                }


            }
            br.close();
            if (flag == 0) {

                String thePath = "";
                thePath = URLEncoder.encode(myQuestion, "UTF-8");
                logger.info("Path {}", thePath);

                HttpClient httpclient = HttpClients.createDefault();
                HttpGet httpget = new HttpGet(
                        meaningCloudUrl + "?key=" + meaningCloudKey + "&of=json&lang=en&ilang=en&txt="
                                + thePath + "&tt=e&uw=y");
                // httpget.addHeader("User-Agent", USER_AGENT);
                HttpResponse response = httpclient.execute(httpget);
                try {

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
                        JSONObject response2 = new JSONObject(text);
                        logger.info("response2: {}", response2);
                        if (response2.has("entity_list")) {
                            JSONArray ents = (JSONArray) response2.get("entity_list");
                            for (int j = 0; j < ents.length(); j++) {
                                JSONObject formObject = (JSONObject) ents.getJSONObject(j);
                                logger.info("formObject_1: {}", formObject);
                                if (formObject.has("variant_list")) {
                                    JSONArray jsonArray = (JSONArray) formObject.get("variant_list");
                                    String link = null;
                                    if (formObject.has("semld_list")) {
                                        JSONArray jsonArray_semld_list = (JSONArray) formObject.get("semld_list");
                                        link = jsonArray_semld_list.getString(0);
                                    }
                                    logger.info("jsonArray_variant_list : {}", jsonArray);
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject explrObject = jsonArray.getJSONObject(i);
                                        logger.info("form_explrObject_2 : {}", explrObject);
                                        int begin = explrObject.getInt("inip");
                                        int end = explrObject.getInt("endp");
                                        logger.info("Question: {}", explrObject);
                                        logger.info("Begin: {}", begin);
                                        logger.info("End: {}", end);
                                        logger.info("link: {}", link);
                                        Selection s = new Selection();
                                        s.begin = begin;
                                        s.end = end;
                                        String finalUrl = "";
                                        if (link != null && link.contains("wikipedia")) {
                                            finalUrl = "http://dbpedia.org/resource" + link.substring(28);
                                        }
                                        logger.info("finalUrl: {}", finalUrl);
                                        s.link = finalUrl;
                                        selections.add(s);
                                    }
                                }
                            }
                        }
                    }
                    BufferedWriter buffWriter = new BufferedWriter(new FileWriter(cacheFilePath, true));
                    Gson gson = new Gson();

                    String json = gson.toJson(selections);
                    logger.info("gsonwala: {}", json);

                    String MainString = myQuestion + " Answer: " + json;
                    buffWriter.append(MainString);
                    buffWriter.newLine();
                    buffWriter.close();
                } catch (JSONException e) {
                    logger.error("Exception: {}", e);
                } catch (ClientProtocolException e) {
                    logger.error("Exception: {}", e);
                    // TODO Auto-generated catch block
                } catch (IOException e1) {
                    logger.error("Except: {}", e1);
                    // TODO Auto-generated catch block
                }
            }
        } catch (FileNotFoundException e) {
            //handle this
            logger.error("{}", e);
        }
        logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
        logger.info("apply vocabulary alignment on outgraph");
        for (Selection s : selections) {

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(s.begin), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(s.end), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("answer", ResourceFactory.createResource(s.link));
            bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
            logger.info("SPARQL query: {}", sparql);
            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        }
        return myQanaryMessage;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    class Selection {
        public int begin;
        public int end;
        public String link;
    }
}
