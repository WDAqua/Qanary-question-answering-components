package eu.wdaqua.qanary.component.aylien.ner;

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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class AylienNER extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(AylienNER.class);

    private final String applicationName;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public AylienNER(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
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
        ArrayList<Selection> selections = new ArrayList<Selection>();
        //ArrayList<Link> links = new ArrayList<Link>();
        logger.info("Question {}", myQuestion);

        String thePath = "";
        thePath = URLEncoder.encode(myQuestion, "UTF-8");
        logger.info("Path {}", thePath);

        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://api.aylien.com/api/v1/concepts?text=" + thePath);
        //httpget.addHeader("User-Agent", USER_AGENT);
        httpget.addHeader("X-AYLIEN-TextAPI-Application-Key", "c7f250facfa39df49bb614af1c7b04f7");
        httpget.addHeader("X-AYLIEN-TextAPI-Application-ID", "6b3e5a8d");
        HttpResponse response = httpclient.execute(httpget);
        try {

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                // String result = getStringFromInputStream(instream);
                String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
                JSONObject response2 = new JSONObject(text);
                logger.info("JA: {}", response2);
                JSONObject concepts = (JSONObject) response2.get("concepts");
                logger.info("JA: {}", concepts);
                ArrayList<String> list = new ArrayList<String>(concepts.keySet());
                logger.info("JA: {}", list);
                for (int i = 0; i < list.size(); i++) {
                    JSONObject explrObj = (JSONObject) concepts.get(list.get(i));
                    if (explrObj.has("surfaceForms")) {
                        JSONArray jsonArray = (JSONArray) explrObj.get("surfaceForms");
                        JSONObject explrObj2 = (JSONObject) jsonArray.get(0);
                        int begin = (int) explrObj2.get("offset");
                        String endString = (String) explrObj2.get("string");
                        int end = begin + endString.length();
                        logger.info("Question: {}", explrObj2);
                        logger.info("Question: {}", begin);
                        logger.info("Question: {}", end);
                        Selection s = new Selection();
                        s.begin = begin;
                        s.end = end;
                        selections.add(s);
                    }
                }
            }
        } catch (ClientProtocolException e) {
            logger.error("Exception: {}", e);
            // TODO Auto-generated catch block
        } catch (IOException e1) {
            logger.error("Except: {}", e1);
            // TODO Auto-generated catch block
        }
        logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));

        logger.info("apply vocabulary alignment on outgraph");
        for (Selection s : selections) {

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(s.begin), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(s.end), XSDDatatype.XSDnonNegativeInteger));
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
    }
}
