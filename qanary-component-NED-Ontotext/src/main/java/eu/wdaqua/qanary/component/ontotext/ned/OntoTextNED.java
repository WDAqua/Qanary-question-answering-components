package eu.wdaqua.qanary.component.ontotext.ned;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties	
 * (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class OntoTextNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(OntoTextNED.class);

	private final String applicationName;

	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	public OntoTextNED(@Value("${spring.application.name}") final String applicationName) throws Exception {
		this.applicationName = applicationName;

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
		ArrayList<Link> links = new ArrayList<Link>();
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("https://tag.ontotext.com/extractor-en/extract");
		httppost.addHeader("X-JwtToken", "<JWT Token goes here>");
		httppost.addHeader("Accept", "application/vnd.ontotext.ces+json");
		httppost.addHeader("Content-Type", "text/plain");
		httppost.setEntity(new StringEntity("What is a test?"));
		HttpResponse response = httpclient.execute(httppost);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instream = entity.getContent();
			String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
			JSONObject jsonObject = new JSONObject(text);
			JSONArray jsonArray = jsonObject.getJSONArray("mentions");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject explrObject = jsonArray.getJSONObject(i);
				int begin = (int) explrObject.get("startOffset");
				int end = (int) explrObject.get("endOffset");
				if (explrObject.has("features")) {
					JSONObject features = (JSONObject) explrObject.get("features");
					if (features.has("exactMatch")) {
						JSONArray uri = features.getJSONArray("exactMatch");
						String uriLink = uri.getString(0);
						Link l = new Link();
						l.begin = begin;
						l.end = end;
						l.link = uriLink;
						links.add(l);
					}
				}
			}
			instream.close();
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
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage,
				myQanaryUtils.getQanaryTripleStoreConnector());
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		ArrayList<Link> links = new ArrayList<Link>();

		logger.info("Question: {}", myQuestion);
		// STEP2
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("https://tag.ontotext.com/extractor-en/extract");
		httppost.addHeader("X-JwtToken", "<JWT Token goes here>");
		httppost.addHeader("Accept", "application/vnd.ontotext.ces+json");
		httppost.addHeader("Content-Type", "text/plain");
		httppost.setEntity(new StringEntity(myQuestion));
		try {
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
				logger.info("response: {}", text);
				JSONObject jsonObject = new JSONObject(text);
				JSONArray jsonArray = jsonObject.getJSONArray("mentions");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject explrObject = jsonArray.getJSONObject(i);
					int begin = (int) explrObject.get("startOffset");
					int end = (int) explrObject.get("endOffset");
					if (explrObject.has("features")) {
						JSONObject features = (JSONObject) explrObject.get("features");
						if (features.has("exactMatch")) {
							JSONArray uri = features.getJSONArray("exactMatch");
							String uriLink = uri.getString(0);
							logger.info("Question: {}", explrObject);
							logger.info("Question: {}", begin);
							logger.info("Question: {}", end);
							Link l = new Link();
							l.begin = begin;
							l.end = end;
							l.link = uriLink;
							links.add(l);
						}
					}
				}
				// JSONObject jsnobject = new JSONObject(text);
				// JSONArray jsonArray = jsnobject.getJSONArray("endOffset");
				// for (int i = 0; i < jsonArray.length(); i++) {
				// JSONObject explrObject = jsonArray.getJSONObject(i);
				// logger.info("JSONObject: {}", explrObject);
				// logger.info("JSONArray: {}", jsonArray.getJSONObject(i));
				// //logger.info("Question: {}", text);
				//
				// }
				logger.info("Question: {}", text);
				logger.info("Question: {}", jsonArray);
				try {
					// todo do something useful
				} finally {
					instream.close();
				}
			}
		} catch (ClientProtocolException e) {
			logger.error("Exception: {}", myQuestion);
			// TODO Auto-generated catch block
		}

		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));

		logger.info("apply vocabulary alignment on outgraph");
		for (

		Link l : links) {

			QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
			bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
			bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
			bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.begin), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.end), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("answer", ResourceFactory.createResource(l.link));
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

	class Link {
		public int begin;
		public int end;
		public String link;
	}
}
