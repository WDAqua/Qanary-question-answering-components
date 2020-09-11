package eu.wdaqua.qanary.relnliod;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.util.ResourceUtils;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class RelNliodRel extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(RelNliodRel.class);

    private final String applicationName;
    private final String textRazorServiceURL;
    private final String textRazorServiceKey;
    private final Boolean cacheEnabled;
    private final String cacheFile;
	private final DbpediaRecordProperty dbpediaRecordProperty;
	private final RemovalList removalList;

	public RelNliodRel(@Value("${spring.application.name}") final String applicationName,
                       @Value("${rel-nliod.cache.enabled}") final Boolean cacheEnabled,
                       @Value("${rel-nliod.cache.file}") final String cacheFile,
                       @Value("${rel-nliod.service.url}") final String textRazorServiceURL,
                       @Value("${rel-nliod.service.key}") final String textRazorServiceKey,
					   final DbpediaRecordProperty dbpediaRecordProperty,
					   final RemovalList removalList) {
        this.applicationName = applicationName;
        this.textRazorServiceURL = textRazorServiceURL;
        this.cacheEnabled = cacheEnabled;
        this.cacheFile = cacheFile;
        this.textRazorServiceKey = textRazorServiceKey;
        this.dbpediaRecordProperty = dbpediaRecordProperty;
        this.removalList = removalList;
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component
     *
     * @throws Exception
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        boolean relationsFlag;
        HashSet<String> dbLinkListSet = new HashSet<>();
        logger.info("process: {}", myQanaryMessage);
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();


        boolean hasCacheResult = false;
        if (cacheEnabled) {
            FileCacheResult cacheResult = readFromCache(myQuestion);
            hasCacheResult = cacheResult.hasCacheResult;
            dbLinkListSet.addAll(cacheResult.links);
        }

        if (!hasCacheResult) {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(textRazorServiceURL);
            httppost.setHeader("x-textrazor-key", textRazorServiceKey);
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("text", myQuestion.toLowerCase()));
            params.add(new BasicNameValuePair("extractors", "relations,words"));
            httppost.setEntity(new UrlEncodedFormEntity(params));
            TextRazorDbSearch textRazorDbSearch = new TextRazorDbSearch(dbpediaRecordProperty, removalList);
            try {
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
                    logger.info(text);
                    JSONObject response1 = (new JSONObject(text)).getJSONObject("response");
                    JSONArray jsonArraySent = (JSONArray) response1.get("sentences");
                    ArrayList<String> arrayListWords = textRazorDbSearch.createArrayWordsList(jsonArraySent);
                    logger.info(arrayListWords.toString());
                    textRazorDbSearch.createPropertyList(response1);
                    textRazorDbSearch.createDbLinkListSet(arrayListWords);
                    dbLinkListSet = textRazorDbSearch.getDbLinkListSet();
                    relationsFlag = textRazorDbSearch.getRelationsFlag();
                    if (dbLinkListSet.isEmpty() && relationsFlag == true) {
                        textRazorDbSearch.createRePropertyList(response1);
                        textRazorDbSearch.createDbLinkListSet(arrayListWords);
                    }
                    dbLinkListSet = textRazorDbSearch.getDbLinkListSet();
                    logger.info("DbLinkListSet: " + dbLinkListSet.toString());


                    if (cacheEnabled) {
                        writeToCache(myQuestion, dbLinkListSet);
                    }
                }
            } catch (ClientProtocolException e) {
                logger.info("Exception: {}", e);
                // TODO Auto-generated catch block
            } catch (IOException e1) {
                logger.info("Except: {}", e1);
                // TODO Auto-generated catch block
            }
        }


        logger.info("apply vocabulary alignment on outgraph {}", myQanaryMessage.getOutGraph());

        // for all URLs found using the called API
        for (String urls : dbLinkListSet) {
            String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                    + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                    + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                    + "prefix dbp: <http://dbpedia.org/property/> "
                    + "INSERT { "
                    + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
                    + "  ?a a qa:AnnotationOfRelation . "
                    + "  ?a oa:hasTarget [ "
                    + "           a    oa:SpecificResource; "
                    + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
                    + "  ] ; "
                    + "     oa:hasBody <" + urls + "> ;"
                    + "     oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; "
                    + "	    oa:annotatedAt ?time  "
                    + "}} "
                    + "WHERE { "
                    + "BIND (IRI(str(RAND())) AS ?a) ."
                    + "BIND (now() as ?time) "
                    + "}";
            logger.info("Sparql query {}", sparql);
            myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
        }

        return myQanaryMessage;

    }

    private FileCacheResult readFromCache(String myQuestion) throws IOException {
        final FileCacheResult cacheResult = new FileCacheResult();
        try {
            File f = ResourceUtils.getFile(cacheFile);
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String line;


            while ((line = br.readLine()) != null && !cacheResult.hasCacheResult) {
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
            logger.info("{}", e);
        }
        return cacheResult;
    }

    private void writeToCache(String myQuestion, HashSet<String> links) throws IOException {
        try {
            BufferedWriter buffWriter = new BufferedWriter(new FileWriter(cacheFile, true));
            String MainString = myQuestion + " Answer: " + links.toString();
            buffWriter.append(MainString);
            buffWriter.newLine();
            buffWriter.close();
        } catch (FileNotFoundException e) {
            //handle this
            logger.info("{}", e);
        }
    }

    class FileCacheResult {
        public HashSet<String> links = new HashSet<>();
        public boolean hasCacheResult;
    }
}