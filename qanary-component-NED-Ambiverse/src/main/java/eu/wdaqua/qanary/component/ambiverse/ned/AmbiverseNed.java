package eu.wdaqua.qanary.component.ambiverse.ned;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.commons.io.IOUtils;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class AmbiverseNed extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(AmbiverseNed.class);
    @Value("${ambiverse.client.id}")
    private String CLIENT_ID;
    @Value("${ambiverse.client.secret}")
    private String CLIENT_SECRET;
    @Value("${ambiverse.access.token.url}")
    private String urlAccessToken;
    @Value("${ambiverse.entity.link.service.plain.url}")
    private String urlEntityLinkServicePlain;
    // application.properties
    private String applicationName;

    private String urlEntityLinkService;
    private String[] accessTokenCmd;

    private String FILENAME_INSERT_ANNOTATION = "insert_one_annotation.rq";

    /*
     * constructor calling super constructor and showing printing the used command
     * into the info console
     */
    public AmbiverseNed(@Value("${spring.application.name}") final String applicationName) {
        super();

        this.applicationName = applicationName;

        urlEntityLinkService = "https://" + urlEntityLinkServicePlain;
        String[] accessTokenCmd = {"curl", "-X", "POST", "--header", "Content-Type: application/x-www-form-urlencoded",
                "-d", "grant_type=client_credentials", "-d", "client_id=" + CLIENT_ID, "-d",
                "client_secret=" + CLIENT_SECRET, urlAccessToken};
        this.accessTokenCmd = accessTokenCmd;
        // show on console what will happen while calling the external server
        logger.info("process string (prepared): {}",
                String.join(" ", accessTokenCmd).replace("Content-Type: ", "Content-Type:"));
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        // String myQuestion = "Who is the wife of Barack Obama ?";
        ArrayList<Selection> selections = new ArrayList<Selection>();

        logger.info("Question: {}", myQuestion);

        String thePath = URLEncoder.encode(myQuestion, "UTF-8");
        logger.debug("thePath: {}", thePath);
        JSONObject msg = new JSONObject();
        msg.put("text", myQuestion);
        msg.put("language", "en");
        String jsonThePath = msg.toString();
        logger.info("JsonPath: {}", jsonThePath);

        try {
            ProcessBuilder process = new ProcessBuilder(accessTokenCmd);
            Process p = process.start();
            InputStream instream = p.getInputStream();
            String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
            logger.info("AccessTokenInfo: {}", text);
            JSONObject accessTokenInfo = new JSONObject(text);
            logger.info("AccessTokenInfo: {}", accessTokenInfo);
            String access_token = accessTokenInfo.getString("access_token");
            logger.info("AccessToken: {}", access_token);

            if (access_token != null) {

                String[] entityLinkCmd = {"curl", "--request", "POST", "--header", "Content-Type: application/json",
                        "--header", "Accept: application/json", "--header", "Authorization: Bearer: " + access_token,
                        "-d", jsonThePath, "--url", urlEntityLinkService};

                logger.info("EntityLinkCmd: {}", Arrays.toString(entityLinkCmd));
                ProcessBuilder processEL = new ProcessBuilder(entityLinkCmd);
                logger.info("ProcessEL: {}", processEL.command());
                Process pEL = processEL.start();
                logger.error("Process PEL: {}", IOUtils.toString(pEL.getErrorStream()));
                InputStream instreamEL = pEL.getInputStream();
                String textEL = IOUtils.toString(instreamEL, StandardCharsets.UTF_8.name());
                JSONObject response = new JSONObject(textEL);
                logger.info("response: {}", response.toString());

                JSONArray jsonArrayEL = response.getJSONArray("matches");
                logger.info("EntityLinkInfoArray: {}", jsonArrayEL.toString());

                JSONArray jsonArrayEL1 = response.getJSONArray("entities");
                logger.info("EntityLinkInfoArray1: {}", jsonArrayEL1.toString());

                for (int j = 0, k = 0; j < jsonArrayEL.length() && k < jsonArrayEL1.length(); j++, k++) {
                    JSONObject explrObjectEL = jsonArrayEL.getJSONObject(j);
                    logger.info("EntityJsonObject: {}", explrObjectEL.toString());
                    int begin = (int) explrObjectEL.get("charOffset");
                    int end = begin + (int) explrObjectEL.get("charLength") - 1;
                    // String url= (String)explrObjectEL.getJSONObject("entity").get("url");
                    // String finalUrl =
                    // "http://dbpedia.org/resource"+url.substring(28).replace("%20", "_");
                    logger.info("Question: {} {}", begin, end);
                    Selection s = new Selection();
                    s.begin = begin;
                    s.end = end;

                    JSONObject explrObjectEL1 = jsonArrayEL1.getJSONObject(j);
                    logger.info("EntityJsonObject1: {}", explrObjectEL1.toString());
                    String url1 = (String) explrObjectEL1.get("url");
                    String url = java.net.URLDecoder.decode(url1, "UTF-8");
                    String finalUrl = "http://dbpedia.org/resource" + url.substring(28).replace(" ", "_");
                    logger.info("Question: {}", finalUrl);
                    // Selection s = new Selection();
                    // s.begin = begin;
                    // s.end = end;
                    s.link = finalUrl;
                    selections.add(s);
                }
            } else {
                logger.error("Access_Token: ", "Access token can not be accessed");
            }

        } catch (JSONException e) {
            logger.error("JSONException: {}", e);
            return myQanaryMessage; // for robustness we are returning the service handler to the pipeline
        } catch (IOException e) {
            logger.error("IOException: {}", e);
            return myQanaryMessage; // for robustness we are returning the service handler to the pipeline
        } catch (Exception e) {
            logger.error("Except: {}", e);
            return myQanaryMessage; // for robustness we are returning the service handler to the pipeline
        }

        logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
        logger.info("apply vocabulary alignment on outgraph");
        for (Selection s : selections) {
            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(s.begin), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(s.end), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(s.link));
            bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
            logger.info("Sparql query {}", sparql);
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