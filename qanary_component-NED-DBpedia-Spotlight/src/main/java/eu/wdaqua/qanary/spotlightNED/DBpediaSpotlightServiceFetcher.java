package eu.wdaqua.qanary.spotlightNED;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;

public class DBpediaSpotlightServiceFetcher {
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcher.class);

	/**
	 * fetch data from the configured DBpedia Spotlight endpoint
	 * 
	 * @param myQanaryQuestion
	 * @param myQanaryUtils
	 * @param myQuestion
	 * @param endpoint
	 * @param minimumConfidence
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
    @Cacheable(value = "json", key="#myQuestion")
	public JsonArray getJsonFromService(QanaryQuestion<String> myQanaryQuestion, QanaryUtils myQanaryUtils,
			String myQuestion, String endpoint, float minimumConfidence) throws ClientProtocolException, IOException {

		String uriGetParameter = "?text=" + URLEncoder.encode(myQuestion, StandardCharsets.UTF_8.toString())
				+ "&confidence=" + minimumConfidence;

		HttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.get() //
				.setUri(endpoint + uriGetParameter) //
				.setHeader(HttpHeaders.ACCEPT, "application/json") //
				.build();
		logger.info("non-cached HTTP request: {}", request.getRequestLine());
		HttpResponse response = client.execute(request);
		HttpEntity responseEntity = response.getEntity();
		String json = EntityUtils.toString(responseEntity);

		// parse the response data as JSON and get the found DBpedia resources
		JsonElement root = JsonParser.parseString(json);
		JsonArray resources = root.getAsJsonObject().get("Resources").getAsJsonArray();

		// check if anything was found
		if (resources.size() == 0) {
			logger.warn("nothing recognized for \"{}\": {}", myQuestion, json);
		}

		return resources;
	}
}
