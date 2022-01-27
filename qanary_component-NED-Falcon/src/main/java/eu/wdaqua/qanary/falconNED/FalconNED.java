package eu.wdaqua.qanary.falconNED;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * represents a wrapper of the DBpedia Spotlight service used as NED annotator
 * 
 * requirements: this Qanary service expects as input a textual question (that
 * is stored in the Qanary triplestore) written using English language
 * 
 * outcome: if DBpedia Spotlight has recognized named entities and was enabled
 * to link them to DBpedia, then this information is added to the Qanary
 * triplestore to be used by following services of this question answering
 * process
 *
 * @author Kuldeep Singh, Dennis Diefenbach, Andreas Both
 */

@Component
public class FalconNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(FalconNED.class);

	private final String applicationName;
	private final String falconAPI = "http://172.18.0.1:5001";

	public FalconNED(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}

	@Bean
	public CacheManagerCustomizer<ConcurrentMapCacheManager> getCacheManagerCustomizer() {
		logger.warn("getCacheManagerCustomizer");
		return new CacheManagerCustomizer<ConcurrentMapCacheManager>() {
			@Override
			public void customize(ConcurrentMapCacheManager cacheManager) {
				cacheManager.setAllowNullValues(false);
			}
		};
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		ArrayList<Link> links = new ArrayList<Link>();

		logger.info("Question: {}", myQuestion);
		try {
			File f = new File(getValidFileAbsoluteLocation("entity_questions.txt"));
			FileReader fr = new FileReader(f);
			BufferedReader br  = new BufferedReader(fr);
			int flag = 0;
			String line;
//

			while((line = br.readLine()) != null && flag == 0) {
				String question = line.substring(0, line.indexOf("Answer:"));
				logger.info("{}", line);
				logger.info("{}", myQuestion);

				if(question.trim().equals(myQuestion))
				{
					String Answer = line.substring(line.indexOf("Answer:")+"Answer:".length());
					logger.info("Here {}", Answer);
					Answer = Answer.trim();
					JSONArray jsonArr =new JSONArray(Answer);
					if(jsonArr.length()!=0)
					{
						for (int i = 0; i < jsonArr.length(); i++)
						{
							JSONObject explrObject = jsonArr.getJSONObject(i);

							logger.info("Question: {}", explrObject);

							Link l = new Link();
							l.begin = (int) explrObject.get("begin");
							l.end = (int) explrObject.get("end");
							l.link= explrObject.getString("link");
							links.add(l);
						}
					}
					flag=1;

					break;
				}


			}
			br.close();
			if(flag==0)
			{


				HttpClient httpclient = HttpClients.createDefault();
				HttpPost httppost = new HttpPost(falconAPI);
				httppost.addHeader("Content-Type", "application/json");

				String json = new JSONObject()
						.put("text", myQuestion).toString();
				StringEntity entitytemp = new StringEntity(json);
				httppost.setEntity(entitytemp);
				try {
					HttpResponse response = httpclient.execute(httppost);
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						InputStream instream = entity.getContent();
						// String result = getStringFromInputStream(instream);
						String text2 = IOUtils.toString(instream, StandardCharsets.UTF_8.name());

						String text = text2.substring(text2.indexOf('{'));
						logger.info("Question: {}", text);
						JSONObject jsonObject = new JSONObject(text);
						JSONArray jsonArray = (JSONArray) jsonObject.get("entities_dbpedia");
						for ( int i =0 ; i < jsonArray.length();i++)
						{
							JSONArray jsonArray1 = (JSONArray) jsonArray.get(i);
							String link_temp = (String) jsonArray1.get(0);
							String link_text = (String) jsonArray1.get(1);
							Link l = new Link();
							l.begin = myQuestion.indexOf(link_text);
							l.end = l.begin + link_text.length();
							l.link = link_temp;
							links.add(l);
							logger.info(l.link);
						}
						logger.info("Question: {}", text);
						//logger.info("Question: {}", jsonArray);
						try {
							// do something useful
						} finally {
							instream.close();
						}
					}

					//BufferedWriter buffWriter = new BufferedWriter(new FileWriter("qanary_component-NED-Falcon/src/main/resources/questions.txt", true));
					Gson gson = new Gson();

					String joson = gson.toJson(links);
					logger.info("gsonwala: {}",json);

					String MainString = myQuestion + " Answer: "+joson;
					//buffWriter.append(MainString);
					//buffWriter.newLine();
					//	buffWriter.close();
				}
				catch (ClientProtocolException e) {
					logger.info("Exception: {}", myQuestion);
					// TODO Auto-generated catch block
				} catch (IOException e1) {
					logger.info("Except: {}", e1);
					// TODO Auto-generated catch block
				}
			}
		}
		catch(FileNotFoundException e)
		{
			//handle this
			logger.info("{}", e);
		}
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)
		String sparqlbind = "";
		int i = 0;
		for (Link l : links) {
			String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
					+ "prefix dbp: <http://dbpedia.org/property/> "
					+ "INSERT { "
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
					+ "  ?a" + i + " a qa:AnnotationOfInstance . " //
					+ "  ?a" + i + " oa:hasTarget [ " //
					+ "           a    oa:SpecificResource; " //
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
					+ "           oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  " //
					+ "           ] " //
					+ "  ] . " //
					+ "  ?a" + i + " oa:hasBody <" + l.link + "> ;" //
					+ "     	 oa:annotatedBy <" + this.applicationName + "> ; " //
					+ "	    	 oa:annotatedAt ?time ; " //
					+ "	}"; // end: graph
			sparqlbind += "  BIND (IRI(str(RAND())) AS ?a" + i + ") .";
			i++;
			sparql += "" //
					+ "} " // end: insert
					+ "WHERE { " //
					+ sparqlbind //
					+ "  BIND (now() as ?time) " //
					+ "}";
			logger.info("Sparql query {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
		}

		return myQanaryMessage;


	}
	class Link {
		public int begin;
		public int end;
		public String link;
	}

	protected String getValidFileAbsoluteLocation(String FileName) throws IOException {

		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath*:" + FileName);
		String fileAbsoluteLocation = "";

		for(Resource r: resources) {
			InputStream inputStream = r.getInputStream();
			File somethingFile = File.createTempFile(r.getFilename(), ".cxl");
			try {
				FileUtils.copyInputStreamToFile(inputStream, somethingFile);
			} finally {
				IOUtils.closeQuietly(inputStream);
			}
			logger.info("File Path is {}", somethingFile.getAbsolutePath());
			fileAbsoluteLocation = somethingFile.getAbsolutePath();
		}



		return fileAbsoluteLocation;
	}

}
