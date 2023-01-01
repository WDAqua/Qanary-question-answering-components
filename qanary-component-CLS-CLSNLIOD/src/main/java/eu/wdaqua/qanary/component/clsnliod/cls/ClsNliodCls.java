package eu.wdaqua.qanary.component.clsnliod.cls;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class ClsNliodCls extends QanaryComponent {

    private final String FILENAME_ANNOTATIONS_FILTERED = "insert_one_annotation_of_class.rq";

    private static final Logger logger = LoggerFactory.getLogger(ClsNliodCls.class);

    private final String applicationName;
    private final Boolean cacheEnabled;
    private final String cacheFile;

    public ClsNliodCls(@Value("${spring.application.name}") final String applicationName,
                       @Value("${cls-clsnliod.cache.enabled}") final Boolean cacheEnabled,
                       @Value("${cls-clsnliod.cache.file}") final String cacheFile) {
        this.applicationName = applicationName;
        this.cacheEnabled = cacheEnabled;
        this.cacheFile = cacheFile;
    }

    public static String runCurlPOSTWithParam(String weburl, String data, String contentType) throws Exception {
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

            logger.info("Curl Response {}", xmlResp);
        } catch (Exception e) {
            logger.error("Error in runCurlPOSTWithParam", e);
        }
        return (xmlResp);
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
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        //ArrayList<Selection> selections = new ArrayList<Selection>();

        String language1 = "en";
        logger.info("Language of the Question: {}", language1);
        HashSet<String> dbLinkListSet = new HashSet<>();


        boolean hasCacheResult = false;
        if (cacheEnabled) {
            FileCacheResult cacheResult = readFromCache(myQuestion);
            hasCacheResult = cacheResult.hasCacheResult;
            dbLinkListSet.addAll(cacheResult.links);
        }


        if (!hasCacheResult) {
            String url = "";
            String data = "";
            String contentType = "application/json";

            url = "http://ws.okbqa.org:1515/templategeneration/rocknrole";
            data = "{  \"string\":\"" + myQuestion + "\",\"language\":\"" + language1 + "\"}";//"{  \"string\": \"Which river flows through Seoul?\",  \"language\": \"en\"}";
            logger.info("data :{}", data);
            logger.info("Component : 21");
            String output1 = "";

            try {
                output1 = ClsNliodCls.runCurlPOSTWithParam(url, data, contentType);
            } catch (Exception e) {
                logger.error("Error run url post with param URL:{}, data:{}, ContentType:{}, Error: {}",
                        url, data, contentType, e);
            }

            logger.info("The output template is: {}", output1);
            PropertyRetrival propertyRetrival = new PropertyRetrival();
            Property property = propertyRetrival.retrival(output1);

            List<MySelection> posLstl = new ArrayList<MySelection>();

            // for class
            for (String wrd : property.classRdf) {
                MySelection ms = new MySelection();
                ms.type = "AnnotationOfClass";
                ms.rsc = "SpecificClass";
                ms.word = wrd;
                ms.begin = myQuestion.indexOf(wrd);
                ms.end = ms.begin + wrd.length();
                posLstl.add(ms);
                logger.info("classRdf: " + wrd);

                logger.info("Apply vocabulary alignment on outgraph");

                String dbpediaClass = null;
                try {
                    String myKey1 = wrd.trim();
                    if (myKey1 != null && !myKey1.equals("")) {
                        logger.info("searchDbLinkInTTL: " + myKey1);
                        for (Entry<String, String> e : DbpediaRecorodClass.get().tailMap(myKey1).entrySet()) {
                            if (e.getKey().contains(myKey1)) {
                                dbpediaClass = e.getValue();
                                break;
                            }
                            ArrayList<String> strArrayList = new ArrayList<String>(Arrays.asList(e.getKey().split("\\s+")));
                            for (String s : strArrayList) {
                                if (myKey1.compareTo(s) == 0) {
                                    dbpediaClass = e.getValue();
                                }
                            }

                            if (dbpediaClass != null)
                                break;

                        }

                    }
                } catch (Exception e) {
                    // logger.info("Except: {}", e);
                    // TODO Auto-generated catch block
                }
                if (dbpediaClass != null)
                    dbLinkListSet.add(dbpediaClass);
                logger.info("searchDbLinkInTTL: " + dbpediaClass);
            }
            if (cacheEnabled) {
                writeToCache(myQuestion, dbLinkListSet);
            }

        }

        logger.info("DbLinkListSet : " + dbLinkListSet.toString());
        logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
        // TODO: insert data in QanaryMessage.outgraph

        logger.info("apply vocabulary alignment on outgraph");
        // TODO: implement this (custom for every component)
        for (String urls : dbLinkListSet) {
            QuerySolutionMap bindings = new QuerySolutionMap();
            // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
            bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindings.add("answer", ResourceFactory.createStringLiteral(urls));
            bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            String sparql = this.loadQueryFromFile(FILENAME_ANNOTATIONS_FILTERED, bindings);
            logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
        }
        return myQanaryMessage;

    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    private FileCacheResult readFromCache(String myQuestion) throws IOException {
        final FileCacheResult cacheResult = new FileCacheResult();
        try {
            File f = new File(cacheFile);
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            int flag = 0;
            String line;

            while ((line = br.readLine()) != null && flag == 0) {
                String question = line.substring(0, line.indexOf("Answer:"));
                logger.info("{}", line);
                logger.info("{}", myQuestion);

                if (question.trim().equals(myQuestion)) {
                    String Answer = line.substring(line.indexOf("Answer:") + "Answer:".length());
                    logger.info("Here {}", Answer);
                    Answer = Answer.trim();
                    Answer = Answer.substring(1, Answer.length() - 1);
                    String[] values = Answer.split(",");
                    for (int i = 0; i < values.length; i++) {
                        cacheResult.links.add(values[i]);
                    }
                    cacheResult.hasCacheResult = true;
                    break;
                }


            }
            br.close();
        } catch (FileNotFoundException e) {
            //handle this
            logger.error("{}", e);
        }
        return cacheResult;
    }

    private void writeToCache(String myQuestion, HashSet<String> dbLinkListSet) throws IOException {
        try {
            BufferedWriter buffWriter = new BufferedWriter(new FileWriter(cacheFile, true));


            String MainString = myQuestion + " Answer: " + dbLinkListSet.toString();
            buffWriter.append(MainString);
            buffWriter.newLine();
            buffWriter.close();
        } catch (FileNotFoundException e) {
            //handle this
            logger.error("{}", e);
        }
    }

    class Selection {
        public int begin;
        public int end;
    }

    class FileCacheResult {
        public HashSet<String> links = new HashSet<String>();
        public boolean hasCacheResult;
    }
}
