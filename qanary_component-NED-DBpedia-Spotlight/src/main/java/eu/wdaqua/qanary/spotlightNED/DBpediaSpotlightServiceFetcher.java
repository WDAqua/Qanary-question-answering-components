package eu.wdaqua.qanary.spotlightNED;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

public class DBpediaSpotlightServiceFetcher {
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcher.class);

	@Autowired
	private RestTemplateWithCaching restTemplate;

	@Autowired
	private CacheOfRestTemplateResponse myCacheOfResponses;

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
	@Deprecated
	@Cacheable(value = "json", key = "#myQuestion")
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
		org.apache.http.HttpEntity responseEntity = response.getEntity();
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

	/**
	 * fetch data from DBpedia Spotlight API, as RestTemplateWithCaching is used the
	 * request are cached locally define qanary.webservicecalls.cache.specs in the
	 * application.properties to control or deactivate the local caching
	 * 
	 * @param myQuestion
	 * @param endpoint
	 * @param minimumConfidence
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public JsonArray getJsonFromService(String myQuestion, String endpoint, float minimumConfidence)
			throws UnsupportedEncodingException {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, "application/json");

		String question = URLEncoder.encode(myQuestion, StandardCharsets.UTF_8.toString());

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint) //
				.queryParam("text", question) //
				.queryParam("confidence", minimumConfidence);

		HttpEntity<String> json = restTemplate.exchange( //
				builder.toUriString(), //
				HttpMethod.GET, //
				new HttpEntity<>(headers), //
				String.class);

		logger.debug("result of {} -->\n{}\n{}", builder.toUriString(), json.getBody(),
				myCacheOfResponses.getNumberOfExecutedRequests());

		// parse the response data as JSON and get the found DBpedia resources
		JsonElement root = JsonParser.parseString(json.getBody());
		JsonArray resources = root.getAsJsonObject().get("Resources").getAsJsonArray();

		return resources;
	}
}
