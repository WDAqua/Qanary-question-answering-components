package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.dbpediaspotlight.ned.exceptions.DBpediaSpotlightJsonParsingNotPossible;
import net.minidev.json.JSONObject;

@Component
public class DBpediaSpotlightServiceFetcher {
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightServiceFetcher.class);

	private final String endpoint;
	private final float minimumConfidence;

	private RestTemplateWithCaching restTemplate;
	private CacheOfRestTemplateResponse myCacheOfResponses;

	public DBpediaSpotlightServiceFetcher(
			@Autowired RestTemplateWithCaching myRestTemplate,
			@Autowired CacheOfRestTemplateResponse myCacheOfResponses,
			@Value("${dbpediaspotlight.endpoint}") String endpoint,
			@Value("${dbpediaspotlight.confidence.minimum}") float minimumConfidence
			) {

		this.restTemplate = myRestTemplate;
		this.myCacheOfResponses = myCacheOfResponses;
		this.endpoint = endpoint;
		this.minimumConfidence = minimumConfidence;
	}


	/**
	 * fetch data from the configured DBpedia Spotlight endpoint
	 * 
	 * @param myQuestion
	 * @return
	 * @throws Exception
	 */
	public JsonArray getJsonFromService(String myQuestion) throws Exception {

		URI uri = createRequestUriWithParameters(myQuestion, this.endpoint, this.minimumConfidence);
		HttpEntity<JSONObject> response = fetchNamedEntitiesFromWebService(this.restTemplate, this.myCacheOfResponses, uri);
		JsonArray resources = getResourcesOfResponse(response, myQuestion);

		return resources;
	}

	URI createRequestUriWithParameters(String myQuestion, String endpoint, float minimumConfidence)
			throws UnsupportedEncodingException, URISyntaxException {
		// TODO: use request builder
		String uriGetParameter = "?text=" + URLEncoder.encode(myQuestion, StandardCharsets.UTF_8.toString())
				+ "&confidence=" + minimumConfidence;

		URI uri = new URI(endpoint + uriGetParameter);
		logger.debug("URL: {}", uri.toString());
		return uri;
	}

	HttpEntity<JSONObject> fetchNamedEntitiesFromWebService(RestTemplate restTemplate,
			CacheOfRestTemplateResponse myCacheOfResponses, URI uri) {
		long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();
		HttpEntity<JSONObject> response = restTemplate.getForEntity(uri, JSONObject.class);

		// show caching status of current request
		if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
			logger.warn("request was cached");
		} else {
			logger.info("request was actually executed");
		}

		Assert.notNull(response);
		Assert.notNull(response.getBody());

		return response;
	}

	JsonArray getResourcesOfResponse(HttpEntity<JSONObject> response, String myQuestion)
			throws DBpediaSpotlightJsonParsingNotPossible {
		JsonArray resources;
		JsonElement root = parseJsonBodyOfResponse(response);

		try {
			// until 2022-11
			// resources = root.getAsJsonObject().get("Resources").getAsJsonArray();
			// from 2022-12
			resources = root.getAsJsonObject().get("Resources").getAsJsonObject().get("Resource").getAsJsonArray();
		} catch (Exception e) {
			// the web service returns no array if the result is empty, this is a workaround
			// to cover such situations
			if (root.getAsJsonObject().get("confidence").getAsDouble() >= 0) {
				resources = new JsonArray();
				logger.info("The web service response for '{}' was empty. However, the JSON response was valid.",
						myQuestion);
			} else {
				logger.error("Parsing of service response failed while retrieving Resources.Resource: {}\n{}",
						e.toString(), response);
				throw new DBpediaSpotlightJsonParsingNotPossible(e.toString() + " regarding " + response);
			}
		}

		// check if anything was found
		if (resources.size() == 0) {
			logger.warn("nothing recognized for \"{}\": {}", myQuestion, response);
		} else {
			logger.warn("{} resources recognized for \"{}\": {}", resources.size(), myQuestion, response);
		}

		return resources;
	}

	JsonElement parseJsonBodyOfResponse(HttpEntity<JSONObject> response) throws DBpediaSpotlightJsonParsingNotPossible {
		String json = response.getBody().toString();
		JsonElement root = null;
		try {
			logger.debug("JsonParser.parseString({})", json);
			root = JsonParser.parseString(json);
		} catch (Exception e) {
			logger.error("Parsing of service response failed: {}\n{}", e.toString(), json);
			throw new DBpediaSpotlightJsonParsingNotPossible(e.toString() + " regarding " + json);
		}
		return root;
	}

	public List<FoundDBpediaResource> getListOfResources(JsonArray resources) throws URISyntaxException {
		List<FoundDBpediaResource> foundDBpediaResources = new LinkedList<>();
		for (int i = 0; i < resources.size(); i++) {
			foundDBpediaResources.add(new FoundDBpediaResource(resources.get(i)));
			logger.debug("found resource ({} of {}): {} at ({},{})", //
					i, resources.size() - 1, //
					foundDBpediaResources.get(i).getResource(), //
					foundDBpediaResources.get(i).getBegin(), //
					foundDBpediaResources.get(i).getEnd() //
			);
		}
		return foundDBpediaResources;
	}
}
