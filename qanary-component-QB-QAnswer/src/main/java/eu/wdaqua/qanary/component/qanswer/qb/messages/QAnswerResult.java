package eu.wdaqua.qanary.component.qanswer.qb.messages;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.swagger.v3.oas.annotations.Hidden;
import net.minidev.json.JSONObject;

public class QAnswerResult {
	private static final Logger logger = LoggerFactory.getLogger(QAnswerResult.class);

	@Hidden
	private com.google.gson.JsonParser jsonParser;

	private URI endpoint;
	private String knowledgebaseId;
	private String language;
	private String question;

	private List<JsonObject> values;

	@Hidden
	public final URI RESOURCETYPEURI;
	@Hidden
	public final URI BOOLEANTYPEURI;
	@Hidden
	public final URI STRINGTYPEURI;

	public QAnswerResult(JSONObject json, String question, URI endpoint, String language, String knowledgebaseId)
			throws URISyntaxException  {
		jsonParser = new JsonParser();

		logger.debug("result: {}", json.toJSONString());

		JsonArray parsedJsonArray = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonArray("queries")
				.getAsJsonArray();

		this.question = question;
		this.language = language;
		this.knowledgebaseId = knowledgebaseId;
		this.endpoint = endpoint;

		this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
		this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
		this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");

		initData(parsedJsonArray);
	}

	/**
	 * init the fields while parsing the JSON data
	 * 
	 * @param answers
	 * @throws URISyntaxException
	 * @throws NoLiteralFieldFoundException
	 */
	private void initData(JsonArray answers) throws URISyntaxException {

		this.values = new LinkedList<JsonObject>();

		for (JsonElement json : answers) {
			values.add(json.getAsJsonObject());
		}

		logger.debug("fetched results: {}", this.values.size());
	}

	public List<JsonObject> getValues() {
		return values;
	}

	public String getKnowledgebaseId() {
		return knowledgebaseId;
	}

	public String getLanguage() {
		return language;
	}

	public URI getEndpoint() {
		return endpoint;
	}

	public String getQuestion() {
		return question;
	}

}
