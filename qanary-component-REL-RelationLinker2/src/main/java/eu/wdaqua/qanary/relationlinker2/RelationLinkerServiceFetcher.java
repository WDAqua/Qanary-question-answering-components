package eu.wdaqua.qanary.relationlinker2;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

import org.springframework.cache.annotation.Cacheable;

public class RelationLinkerServiceFetcher {

	private static final Logger logger = LoggerFactory.getLogger(RelationLinkerServiceFetcher.class);

    @Cacheable(value = "json", key="#question")
	public ArrayList<Link> getLinksForQuestion( String question, String endpoint) throws Exception {

		ArrayList<Link> links = new ArrayList<Link>();

		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(endpoint);
		httppost.addHeader("Accept", "application/json");

		httppost.setEntity(new StringEntity(question));
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
				ArrayList<String> list = new ArrayList<String>(jsonObject.keySet());
				JSONObject jsonObj = (JSONObject) jsonObject.get(list.get(0));
				logger.info("test {}", list);
				logger.info("test {}", jsonObj);
				JSONArray jsonArray = (JSONArray) jsonObj.get("0");
				logger.info("test {}", jsonArray);
				String test = (String) jsonArray.get(0);
				Link l = new Link();
				l.begin = question.indexOf(list.get(0));
				l.end = l.begin + list.get(0).length();
				l.link = test;
				links.add(l);
				// for (int i = 0; i < jsonArray.length(); i++) {
				// JSONObject explrObject = jsonArray.getJSONObject(i);
				// int begin = (int) explrObject.get("startOffset");
				// int end = (int) explrObject.get("endOffset");
				// if(explrObject.has("features"))
				// {
				// JSONObject features =(JSONObject) explrObject.get("features");
				// if(features.has("exactMatch"))
				// {
				// JSONArray uri = features.getJSONArray("exactMatch");
				// String uriLink = uri.getString(0);
				// logger.info("Question: {}", explrObject);
				// logger.info("Question: {}", begin);
				// logger.info("Question: {}", end);
				// Link l = new Link();
				// l.begin = begin;
				// l.end = end;
				// l.link = uriLink;
				// links.add(l);
				// }
				// }
				//
				//
				// }
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
				// logger.info("Question: {}", jsonArray);
				try {
					// do something useful
				} finally {
					instream.close();
				}
			}
			return links;
		} catch (ClientProtocolException e) {
			throw new Exception("could not get links for question " + question 
					+ "\n" + e.getLocalizedMessage());
		}

	}

	class Link {
		public int begin;
		public int end;
		public String link;
	}

}
