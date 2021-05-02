package eu.wdaqua.opentapiocaNED;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class OpenTapiocaServiceFetcher {
	private static final Logger logger = LoggerFactory.getLogger(OpenTapiocaServiceFetcher.class);

	public JsonArray getJsonFromService( 
			String myQuestion, String endpoint) throws ClientProtocolException, IOException {

		String uriGetParameter = "query=" + URLEncoder.encode(
				myQuestion, StandardCharsets.UTF_8.toString());

		HttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.post() //
			.setUri(endpoint)	//
			.setHeader(HttpHeaders.ACCEPT, "applcation/json") //
			.setEntity(new StringEntity(uriGetParameter))
			.build();
		logger.info("HTTP request: {}", request.getRequestLine());
		HttpResponse response = client.execute(request);
		HttpEntity responseEntity = response.getEntity();
		String json = EntityUtils.toString(responseEntity);

		// parse the response data as JSON to get Wikidata resources
		JsonElement root = JsonParser.parseString(json);
		JsonArray resources = root.getAsJsonObject().get("annotations").getAsJsonArray();

		// check for empty results
		if (resources.size() == 0) {
			logger.warn("nothing recognized for \"{}\": {}", myQuestion, json);
		}

		return resources;
	}
}
