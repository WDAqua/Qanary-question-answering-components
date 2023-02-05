package eu.wdaqua.qanary.component.tagme.ned;

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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * This component retrieved named entities for a given question from the Tagme
 * Web service
 */
public class TagmeNED extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(TagmeNED.class);

    private final String applicationName;
    private final String tagMeServiceURL;
    private final Boolean cacheEnabled;
    private final String cacheFile;
    private float tagMeMinimumLinkPropability;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public TagmeNED(@Value("${spring.application.name}") final String applicationName,
                    @Value("${ned-tagme.cache.enabled}") final Boolean cacheEnabled,
                    @Value("${ned-tagme.cache.file}") final String cacheFile,
                    @Value("${ned-tagme.service.url}") final String tagMeServiceURL,
                    @Value("${ned-tagme.link_propability.threshold:0.25}") final float tagMeMinimumLinkPropability) {
        this.applicationName = applicationName;
        this.tagMeServiceURL = tagMeServiceURL;
        this.cacheEnabled = cacheEnabled;
        this.cacheFile = cacheFile;
        this.tagMeMinimumLinkPropability = tagMeMinimumLinkPropability;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
    }

    /**
     * process the request from the Qanary pipeline
     *
     * @throws Exception
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);

        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        String myQuestion = myQanaryQuestion.getTextualRepresentation();

        List<NamedEntity> links = new ArrayList<>();

        logger.info("Question: {}", myQuestion);
        boolean hasCacheResult = false;
        if (cacheEnabled) {
            FileCacheResult cacheResult = readFromCache(myQuestion);
            hasCacheResult = cacheResult.hasCacheResult;
            links.addAll(cacheResult.links);
        }

        if (!hasCacheResult) {
            links = retrieveDataFromWebService(myQuestion);
        }

        logger.warn("No entities found and >= the threshold of {}.", tagMeMinimumLinkPropability);
        logger.info("Store data ({} found entities) in graph {}.", links.size(), myQanaryMessage.getEndpoint());

        for (NamedEntity l : links) {

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.getBegin()), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.getLinkProbability()), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("score", ResourceFactory.createTypedLiteral(String.valueOf(l.getLinkProbability()), XSDDatatype.XSDfloat));
            bindingsForInsert.add("answer", ResourceFactory.createResource(l.getLink().toString()));
            bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
            logger.info("SPARQL query: {}", sparql);
            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
        }
        return myQanaryMessage;
    }

    public List<NamedEntity> retrieveDataFromWebService(String myQuestion) throws IOException {
        ArrayList<NamedEntity> links = new ArrayList<>();
        logger.info("Question {}", myQuestion);

        String thePath = "";
        thePath = URLEncoder.encode(myQuestion, "UTF-8");
        logger.info("Path {}", thePath);

        HttpClient httpclient = HttpClients.createDefault();
        String serviceUrl = tagMeServiceURL + thePath;
        logger.info("Service call: {}", serviceUrl);
        HttpGet httpget = new HttpGet(serviceUrl);

        HttpResponse response = httpclient.execute(httpget);
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();

                String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
                JSONObject response2 = new JSONObject(text);
                logger.info("response2: {}", response2);
                if (response2.has("annotations")) {
                    JSONArray jsonArray = (JSONArray) response2.get("annotations");
                    if (jsonArray.length() != 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject explrObject = jsonArray.getJSONObject(i);
                            int begin = (int) explrObject.get("start");
                            int end = (int) explrObject.get("end");
                            double linkProbability = explrObject.getDouble("link_probability");
                            String uri = (String) explrObject.get("title");
                            String finalUri = "http://dbpedia.org/resource/" + uri.replace(" ", "_");

                            NamedEntity foundNamedEntity = new NamedEntity(finalUri, begin, end + 1, linkProbability);

                            logger.info("Found Named Entity: {}", foundNamedEntity);
                            logger.debug("Found Named Entity data: {}", explrObject);

                            if (linkProbability >= tagMeMinimumLinkPropability) {
                                logger.info("Adding link_probability >= 0.65 uri {}", finalUri);
                                links.add(foundNamedEntity);
                            } else {
                                logger.warn("link_probability was too low ({} < {}) for {}", linkProbability,
                                        tagMeMinimumLinkPropability, finalUri);
                            }

                        }
                    }
                }
            }

            if (cacheEnabled) {
                writeToCache(myQuestion, links);
            }
        } catch (ClientProtocolException e) {
            logger.info("Exception: {}", e);
        } catch (IOException e1) {
            logger.info("Except: {}", e1);
        }

        return links;
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
                    String answer = line.substring(line.indexOf("Answer:") + "Answer:".length());
                    logger.info("Here {}", answer);
                    answer = answer.trim();
                    JSONArray jsonArr = new JSONArray(answer);
                    if (jsonArr.length() != 0) {
                        for (int i = 0; i < jsonArr.length(); i++) {
                            JSONObject explrObject = jsonArr.getJSONObject(i);

                            logger.info("Question: {}", explrObject);

                            NamedEntity l = new NamedEntity(explrObject.getString("link"), (int) explrObject.get("begin"), (int) explrObject.get("end") + 1);
                            cacheResult.links.add(l);
                        }
                    }
                    cacheResult.hasCacheResult = true;
                    logger.info("hasCacheResult {}", cacheResult.hasCacheResult);

                    break;
                }

            }
            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            // handle this
            logger.info("{}", e);
        }
        return cacheResult;
    }

    private void writeToCache(String myQuestion, ArrayList<NamedEntity> links) throws IOException {
        try {
            BufferedWriter buffWriter = new BufferedWriter(
                    new FileWriter(cacheFile, true));
            Gson gson = new Gson();

            String json = gson.toJson(links);
            logger.info("gsonwala: {}", json);

            String mainString = myQuestion + " Answer: " + json;
            buffWriter.append(mainString);
            buffWriter.newLine();
            buffWriter.close();
        } catch (FileNotFoundException e) {
            // handle this
            logger.info("{}", e);
        }
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    class FileCacheResult {
        public ArrayList<NamedEntity> links = new ArrayList<>();
        public boolean hasCacheResult;
    }
}
