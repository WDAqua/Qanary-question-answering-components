package eu.wdaqua.qanary.component.dandelion.ned;

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
 * The Qanary pipeline endpoint defined in application.properties
 * (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class DandelionNED extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(DandelionNED.class);

    private final String applicationName;

	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	public DandelionNED(@Value("${spring.application.name}") final String applicationName) throws Exception {
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
		String myQuestion = "What is a test?";

		ArrayList<Link> links = new ArrayList<Link>();

		String thePath = "";
		thePath = URLEncoder.encode(myQuestion, "UTF-8");

		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet("https://api.dandelion.eu/datatxt/nex/v1/?text=" + thePath
				+ "&include=types%2Cabstract%2Ccategories&token=0990bd650d9545709da047537ff05a49");
		// httpget.addHeader("User-Agent", USER_AGENT);
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instream = entity.getContent();
			// String result = getStringFromInputStream(instream);
			String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
			JSONObject response2 = new JSONObject(text);
			if (response2.has("annotations")) {
				JSONArray jsonArray = (JSONArray) response2.get("annotations");
				if (jsonArray.length() != 0) {
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject explrObject = jsonArray.getJSONObject(i);
						int begin = (int) explrObject.get("start");
						int end = (int) explrObject.get("end");
						String uri = (String) explrObject.get("uri");
						String finalUri = "http://dbpedia.org/resource" + uri.substring(28);

						Link l = new Link();
						l.begin = begin;
						l.end = end + 1;
						l.link = finalUri;
						links.add(l);
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
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		ArrayList<Link> links = new ArrayList<Link>();

		logger.info("Question {}", myQuestion);

		String thePath = "";
		thePath = URLEncoder.encode(myQuestion, "UTF-8");
		logger.info("Path {}", thePath);

		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet("https://api.dandelion.eu/datatxt/nex/v1/?text=" + thePath
				+ "&include=types%2Cabstract%2Ccategories&token=0990bd650d9545709da047537ff05a49");
		// httpget.addHeader("User-Agent", USER_AGENT);
		HttpResponse response = httpclient.execute(httpget);
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				// String result = getStringFromInputStream(instream);
				String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
				JSONObject response2 = new JSONObject(text);
				logger.info("JA: {}", response2);
				if (response2.has("annotations")) {
					JSONArray jsonArray = (JSONArray) response2.get("annotations");
					if (jsonArray.length() != 0) {
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject explrObject = jsonArray.getJSONObject(i);
							int begin = (int) explrObject.get("start");
							int end = (int) explrObject.get("end");
							logger.info("Begin: {}", begin);
							logger.info("End: {}", end);
							String uri = (String) explrObject.get("uri");
							String finalUri = "http://dbpedia.org/resource" + uri.substring(28);
							logger.info("Link {}", finalUri);

							Link l = new Link();
							l.begin = begin;
							l.end = end + 1;
							l.link = finalUri;
							links.add(l);
						}
					}
				}
			}
		} catch (ClientProtocolException e) {
			logger.info("Exception: {}", e);
			// TODO Auto-generated catch block
		}

		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		for (Link l : links) {
			QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
			bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
			bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
			bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.begin), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.end), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(l.link));
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
