package eu.wdaqua.qanary.aylien;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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


public class AylienServiceFetcher {

	private static final Logger logger = LoggerFactory.getLogger(AylienServiceFetcher.class);

	public ArrayList<Link> getLinksForQuestion(String endpoint, String myQuestion) throws Exception {
		ArrayList<Link> links = new ArrayList<Link>();
		try {

			logger.info("Question {}", myQuestion);
			String thePath = URLEncoder.encode(myQuestion, "UTF-8");
			logger.info("Path {}", thePath);

			HttpClient httpclient = HttpClients.createDefault();
			HttpGet httpget = new HttpGet(endpoint + thePath);
			logger.info("fetching links for Question with URL: {}", endpoint+thePath);
			// httpget.addHeader("User-Agent", USER_AGENT);
			httpget.addHeader("X-AYLIEN-TextAPI-Application-Key", "c7f250facfa39df49bb614af1c7b04f7");
			httpget.addHeader("X-AYLIEN-TextAPI-Application-ID", "6b3e5a8d");
			HttpResponse response = httpclient.execute(httpget);
			logger.info("RESPONSE STATUS: {}", response.getStatusLine());
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream instream = entity.getContent();
					// String result = getStringFromInputStream(instream);
					String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
					JSONObject response2 = new JSONObject(text);
					// logger.info("JA: {}", response2);
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
							// logger.info("Question: {}", explrObj2);
							logger.info("Start: {}", begin);
							logger.info("End: {}", end);
							String finalUri = list.get(i);

							Link l = new Link();
							l.begin = begin;
							l.end = end;
							l.link = finalUri;
							links.add(l);
						}
					}
				}
			} catch (ClientProtocolException e) {
				logger.info("Exception: {}", e);
				// TODO Auto-generated catch block
			}

		} catch (FileNotFoundException e) {
			// handle this
			logger.info("{}", e);
		}
		return links;
	}

	class Link {
		public int begin;
		public int end;
		public String link;
	}
}

