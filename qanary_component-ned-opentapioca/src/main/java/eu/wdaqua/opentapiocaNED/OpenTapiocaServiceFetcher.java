package eu.wdaqua.opentapiocaNED;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.swagger.v3.oas.annotations.Operation;

public class OpenTapiocaServiceFetcher {

	private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaServiceFetcher.class);

	/**
	 * Perform a POST request with the provided question to the specified endpoint
	 *
	 * @param myQuestion the question Text
	 * @param endpoint the endpoint to be used
	 * @return resources the query results as JsonArray 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Operation(
		summary = "Query OpenTapioca endpoint", //
		operationId = "getJsonFromService", //
		description = "Perform a POST request with the provided question to the specified endpoint" //
	)
	public JsonArray getJsonFromService( 
			String myQuestion, String endpoint) throws ClientProtocolException, IOException {

		Assert.notNull(myQuestion, "Question must not be null");

		String uriGetParameter = "query=" + URLEncoder.encode(
				myQuestion, StandardCharsets.UTF_8.toString());

		// build the request
		HttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.post() //
			.setUri(endpoint)	//
			.setHeader(HttpHeaders.ACCEPT, "applcation/json") //
			.setEntity(new StringEntity(uriGetParameter))
			.build();
		HttpResponse response = client.execute(request);
		HttpEntity responseEntity = response.getEntity();

		// parse the response data as JSON to get Wikidata resources
		String json = EntityUtils.toString(responseEntity);
		JsonElement root = JsonParser.parseString(json);
		JsonArray resources = root.getAsJsonObject().get("annotations").getAsJsonArray();

		logger.info("found {} annotations for \"{}\"", resources.size(), myQuestion);

		return resources;
	}

	/**
	 * Create FoundWikidataResource objects from the JSON response of OpenTapioca endpoint
	 *
	 * @param resources JsonArray of resources
	 * @return foundWikidataResources list of FoundWikidataResource objects
	 * @throws URISyntaxException
	 */
	@Operation(
		summary = "Parse the OpenTapioca endpoint reponse", //
		operationId = "parseOpenTapiocaResults", //
		description = "Create FoundWikidataResource objects from the JSON response of OpenTapioca endpoint" //
	)
	public List<FoundWikidataResource> parseOpenTapiocaResults(JsonArray resources) throws URISyntaxException {

		List<FoundWikidataResource> foundWikidataResources = new LinkedList<>();
		logger.info("found {} terms", resources.size());

		for (int i = 0; i < resources.size(); i++) {
			// this layer only contains information about the surface form: the part of the 
			// question String for which one or multiple entities were found.
			
			JsonObject currentTerm = resources.get(i).getAsJsonObject();
			int start = currentTerm.get("start").getAsInt();
			int end = currentTerm.get("end").getAsInt();

			// the second layer contains all entities that were identified for that surface form.
			JsonArray tags = currentTerm.get("tags").getAsJsonArray();
			for (int j = 0; j < tags.size(); j++) {
				JsonObject entity = tags.get(j).getAsJsonObject();
				double score = entity.get("rank").getAsDouble();
				String qid = entity.get("id").getAsString();
				URI resource = new URI("http://www.wikidata.org/entity/" + qid); 

				// hold the information for every individual entity
				foundWikidataResources.add(new FoundWikidataResource(start, end, score, resource));
			}
		}
		return foundWikidataResources;
	}
}
