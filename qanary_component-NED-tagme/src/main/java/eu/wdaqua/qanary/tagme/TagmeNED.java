package eu.wdaqua.qanary.tagme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.google.gson.Gson;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

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
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
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
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>  \n" //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" //
					+ "INSERT { \n" //
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { \n" //
					+ "  ?a a qa:AnnotationOfInstance . \n" //
					+ "  ?a oa:hasTarget [ \n" //
					+ "           a    oa:SpecificResource; \n" //
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; \n" //
					+ "           oa:hasSelector  [ \n" //
					+ "                    a oa:TextPositionSelector ; \n" //
					+ "                    oa:start \"" + l.getBegin() + "\"^^xsd:nonNegativeInteger ; \n" //
					+ "                    oa:end  \"" + l.getEnd() + "\"^^xsd:nonNegativeInteger ; \n" //
					+ "                    qa:score \"" + l.getLinkProbability() + "\"^^xsd:float \n" //
					+ "           ] \n" //
					+ "  ] . \n" //
					+ "  ?a oa:hasBody <" + l.getLink() + "> ; \n" //
					+ "     oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; \n" //
					+ "	    oa:annotatedAt ?time  " + "}} \n" //
					+ "WHERE { \n" //
					+ "  BIND (IRI(str(RAND())) AS ?a) . \n" //
					+ "  BIND (now() as ?time) \n" //
					+ "} \n";
			logger.debug("SPARQL query: {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
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

							NamedEntity foundNamedEntity = new NamedEntity(finalUri, begin, end+1, linkProbability);

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
					new FileWriter("qanary_component-NED-tagme/src/main/resources/questions.txt", true));
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

	class FileCacheResult {
		public ArrayList<NamedEntity> links = new ArrayList<>();
		public boolean hasCacheResult;
	}
}
