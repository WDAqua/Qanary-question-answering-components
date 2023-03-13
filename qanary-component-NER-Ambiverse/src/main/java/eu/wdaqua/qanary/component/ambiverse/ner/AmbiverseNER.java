package eu.wdaqua.qanary.component.ambiverse.ner;

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
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class AmbiverseNER extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(AmbiverseNER.class);
	@Value("${ambiverse.client.id}")
	private String CLIENT_ID;

	@Value("${ambiverse.client.secred}")
	private String CLIENT_SECRET;

	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	private final String applicationName;

	public AmbiverseNER(@Value("${spring.application.name}") final String applicationName) {
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
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		//String myQuestion = "Who is the wife of Barak Obama ?";
		ArrayList<Selection> selections = new ArrayList<Selection>();
		logger.info("Question {}", myQuestion);
		String thePath = URLEncoder.encode(myQuestion, "UTF-8");
		String jsonThePath = (new JSONObject()).put("text", thePath).toString();

		logger.info("JsonPath {}", jsonThePath);

		String urlAccessToken = "https://api.ambiverse.com/oauth/token";
		String urlEntityLinkService = "https://api.ambiverse.com/v2/entitylinking/analyze";
		String[] accessTokenCmd = {"curl", "-X", "POST", "-H",
				"Content-Type: application/x-www-form-urlencoded",
				"-d", "grant_type=client_credentials",
				"-d", "client_id=" + CLIENT_ID,
				"-d", "client_secret=" + CLIENT_SECRET, urlAccessToken};

		try {
			ProcessBuilder process = new ProcessBuilder(accessTokenCmd);
			Process p = process.start();
			InputStream instream = p.getInputStream();
			String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
			logger.info("AccessTokenInfo: {}", text);
			JSONObject accessTokenInfo = new JSONObject(text);
			logger.info("AccessTokenInfo: {}", accessTokenInfo);
			String access_token = accessTokenInfo.getString("access_token");
			logger.info("AccessToken:  {}", access_token);

			if (access_token != null) {

				String[] entityLinkCmd = {"curl", "--compressed", "-X", "POST", "-H", "Content-Type: application/json",
						"-H", "Accept: application/json", "-H",
						"Authorization:" + access_token, "-d", jsonThePath, urlEntityLinkService};


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

				for (int j = 0; j < jsonArrayEL.length(); j++) {
					JSONObject explrObjectEL = jsonArrayEL.getJSONObject(j);
					logger.info("EntityJsonObject:{}", explrObjectEL.toString());
					int begin = (int) explrObjectEL.get("charOffset");
					int end = begin + (int) explrObjectEL.get("charLength") - 1;
					logger.info("Question: {}", begin);
					logger.info("Question: {}", end);
					Selection s = new Selection();
					s.begin = begin;
					s.end = end;
					selections.add(s);
				}
			} else {

				logger.error("Access_Token: {}", "Access token can not be accessed");

			}

		} catch (JSONException e) {
			logger.error("Except: {}", e);

		} catch (IOException e) {
			logger.error("Except: {}", e);
			// TODO Auto-generated catch block
		} catch (Exception e) {
			logger.error("Except: {}", e);
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
