package eu.wdaqua.qanary.mypackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import org.springframework.cache.annotation.Cacheable;

public class BabelfyServiceFetcher {

	private static Logger logger = LoggerFactory.getLogger(BabelfyServiceFetcher.class);

    @Cacheable(value = "json", key="#question")
    public ArrayList<Link> getLinksForQuestion(String endpoint, String myQuestion, String params) throws Exception {

		ArrayList<Link> links = new ArrayList<Link>();
		String thePath = "";
		thePath = URLEncoder.encode(myQuestion, "UTF-8");
		logger.info("Path {}", thePath);

		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(endpoint + thePath + params);
		HttpResponse response = httpclient.execute(httpget);
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				// String result = getStringFromInputStream(instream);
				String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
				JSONArray jsonArray = new JSONArray(text);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject explrObject = jsonArray.getJSONObject(i);
					logger.info("JSON {}", explrObject);
					double score = (double) explrObject.get("score");
					if (score >= 0.5) {
						JSONObject char_array = explrObject.getJSONObject("charFragment");
						int begin = (int) char_array.get("start");
						int end = (int) char_array.get("end");
						logger.info("Begin: {}", begin);
						logger.info("End: {}", end);

						Link l = new Link();
						l.begin = begin;
						l.end = end + 1;
						l.link = (String) explrObject.get("DBpediaURL");
						links.add(l);
					}
				}
			}
			return links;
		} catch (ClientProtocolException e) {
			throw new Exception("could not get links for question " + myQuestion
					+ "\n" + e.getLocalizedMessage());
		}
    }

	class Link {
		public int begin;
		public int end;
		public String link;
	}
}
