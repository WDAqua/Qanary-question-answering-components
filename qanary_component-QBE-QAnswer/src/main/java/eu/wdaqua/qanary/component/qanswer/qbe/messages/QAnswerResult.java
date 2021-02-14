package eu.wdaqua.qanary.component.qanswer.qbe.messages;

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

	private com.google.gson.JsonParser jsonParser;

	private URI endpoint;
	private String knowledgebaseId;
	private String language;
	private String question;

	private String sparql;
	private List<String> values;
	private String type;
	private URI datatype;
	private double confidence;
	
	@Hidden
	public final URI RESOURCETYPEURI;  
	@Hidden
	public final URI BOOLEANTYPEURI;  
	@Hidden
	public final URI STRINGTYPEURI;  

	public QAnswerResult(JSONObject json, String question, URI endpoint, String language, String knowledgebaseId) throws URISyntaxException, NoLiteralFieldFoundException {
		jsonParser = new JsonParser();
		JsonArray parsedJsonArray = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonArray("questions")
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

	@Hidden
	public boolean isAnswerOfResourceType() {
		if( this.getDatatype().equals(RESOURCETYPEURI)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Hidden
	public boolean isAnswerOfBooleanType() {
		if( this.getDatatype().equals(BOOLEANTYPEURI)) {
			return true;
		} else {
			return false;
		}
	}

	@Hidden
	public boolean isAnswerOfLiteralType() {
		return !isAnswerOfResourceType();
	}
	
	/**
	 * init the fields while parsing the JSON data
	 * 
	 * @param answers
	 * @throws URISyntaxException
	 * @throws NoLiteralFieldFoundException 
	 */
	private void initData(JsonArray answers) throws URISyntaxException, NoLiteralFieldFoundException {
		if (isAnswerLiteral(answers)) {
			initDataLiteral(answers);
		} else if (isAnswerResources(answers)) { // ASK QUERY
			initDataResources(answers);
		} else if (isAnswerBoolean(answers)) {
			initDataBoolean(answers);
		} else {
			throw new RuntimeException("case not implemented");
		}
		
		JsonObject questionData = answers.get(0).getAsJsonObject().get("question").getAsJsonObject();
		logger.debug("responseQuestion: {}", questionData);

		JsonArray languages = questionData.get("language").getAsJsonArray();
		logger.debug("responseQuestion->language: {}", languages.toString());
		
		JsonObject language = languages.get(0).getAsJsonObject(); 
		logger.debug("0. language: {}", language.toString());
		logger.debug("0. sparql: {}", language.get("SPARQL").getAsString());
		logger.debug("0. confidence: {}", language.get("confidence").getAsDouble());

		this.confidence = language.get("confidence").getAsDouble();
		this.sparql = language.get("SPARQL").getAsString();
		
	}

	private void initDataLiteral(JsonArray answers) throws URISyntaxException, NoLiteralFieldFoundException {
		ProcessedResult result = getDataLiteral(answers);
		initData(result);
	}

	private ProcessedResult getDataLiteral(JsonArray answers) throws URISyntaxException, NoLiteralFieldFoundException {
		JsonObject questionData = answers.get(0).getAsJsonObject().get("question").getAsJsonObject();
		logger.debug("responseQuestion: {}", questionData);

		JsonObject concreteAnswers = jsonParser.parse(questionData.get("answers").getAsString()).getAsJsonObject();
		logger.debug("responseQuestion->answers: {}", concreteAnswers.toString());

		JsonObject results = concreteAnswers.get("results").getAsJsonObject();
		logger.debug("results: {}", results);

		JsonArray vars = concreteAnswers.get("head").getAsJsonObject().get("vars").getAsJsonArray();
		String resultKey = vars.get(0).getAsString();
		logger.debug("vars: {}, key: {}", vars, resultKey);

		JsonArray bindings = results.get("bindings").getAsJsonArray();
		logger.debug("bindings: {}", bindings);

		String type = null;
		URI datatype = null;

		List<String> values = new LinkedList<>();
		int count = 0;
		for (JsonElement resource : bindings) {
			JsonObject resourceResult = resource.getAsJsonObject().get(resultKey).getAsJsonObject();
			logger.info("found {}: {} -> {}", count++, resourceResult.get("type").getAsString(),
					resourceResult.get("value").getAsString());
			type = resourceResult.get("type").getAsString();
			if ( !type.equals("literal")) {
				throw new NoLiteralFieldFoundException(type);
			}
			try {
				datatype = new URI(resourceResult.get("datatype").getAsString());
			} catch (Exception e) {
				datatype = STRINGTYPEURI;
			}
			values.add(resourceResult.get("value").getAsString());
		}
		logger.info("found {} literals", values.size());
		return new ProcessedResult(values, type, datatype);
	}

	private boolean isAnswerLiteral(JsonArray answers) {
		try {
			ProcessedResult result = getDataLiteral(answers);
			logger.info("result: IS of type 'literal' ({}).", result.getDatatype());
			return true;
		} catch (Exception e) {
			logger.info("result: Is NOT of type 'literal'.");
			return false;
		}
	}

	private void initData(ProcessedResult result) {
		this.values = result.getValues();
		this.type = result.getType();
		this.datatype = result.getDatatype();
		// TODO: SPARQL query
	}

	private void initDataResources(JsonArray answers) throws URISyntaxException {
		ProcessedResult result = getDataResources(answers);
		initData(result);
	}

	private ProcessedResult getDataResources(JsonArray answers) throws URISyntaxException {
		JsonObject questionData = answers.get(0).getAsJsonObject().get("question").getAsJsonObject();
		logger.debug("responseQuestion: {}", questionData);

		JsonObject concreteAnswers = jsonParser.parse(questionData.get("answers").getAsString()).getAsJsonObject();
		logger.debug("responseQuestion->answers: {}", concreteAnswers.toString());

		JsonObject results = concreteAnswers.get("results").getAsJsonObject();
		logger.debug("results: {}", results);

		JsonArray vars = concreteAnswers.get("head").getAsJsonObject().get("vars").getAsJsonArray();
		String resultKey = vars.get(0).getAsString();
		logger.debug("vars: {}, key: {}", vars, resultKey);

		JsonArray bindings = results.get("bindings").getAsJsonArray();
		logger.debug("bindings: {}", bindings);

		String type = null;
		List<String> values = new LinkedList<>();
		int count = 0;
		for (JsonElement resource : bindings) {
			JsonObject resourceResult = resource.getAsJsonObject().get(resultKey).getAsJsonObject();
			logger.info("found {}: {} -> {}", count++, resourceResult.get("type").getAsString(),
					resourceResult.get("value").getAsString());
			type = resourceResult.get("type").getAsString();
			values.add(resourceResult.get("value").getAsString());
		}

		logger.info("found {} resources", values.size());
		return new ProcessedResult(values, type, this.RESOURCETYPEURI);
	}

	private boolean isAnswerResources(JsonArray answers) {
		try {
			getDataResources(answers);
			logger.info("result: IS of type 'resources'.");
			return true;
		} catch (Exception e) {
			logger.info("result: Is NOT of type 'resources'.");
			return false;
		}
	}

	/**
	 * process boolean data
	 * 
	 * @param answers
	 * @throws URISyntaxException
	 */
	private void initDataBoolean(JsonArray answers) throws URISyntaxException {
		ProcessedResult result = getDataBoolean(answers);
		initData(result);
	}

	private ProcessedResult getDataBoolean(JsonArray answers) throws URISyntaxException {
		JsonObject questionData = answers.get(0).getAsJsonObject().get("question").getAsJsonObject();
		logger.debug("questionData: {}", questionData);

		com.google.gson.JsonParser jsonParser = new JsonParser();
		JsonObject parsedAnswer = jsonParser.parse(questionData.toString()).getAsJsonObject();
		logger.debug("parsedAnswer: {}", parsedAnswer);

		JsonObject concreteAnswer = jsonParser.parse(parsedAnswer.get("answers").getAsString()).getAsJsonObject();
		logger.debug("concreteAnswer: {}", concreteAnswer);

		boolean result = concreteAnswer.get("boolean").getAsBoolean();
		logger.debug("boolean result: {}", result);

		List<String> values = new LinkedList<>();
		values.add("" + result);

		return new ProcessedResult(values, "boolean", this.BOOLEANTYPEURI);
	}

	private boolean isAnswerBoolean(JsonArray answers) {
		try {
			getDataBoolean(answers);
			logger.info("result: IS of type 'resources'.");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public List<String> getValues() {
		return values;
	}

	public String getType() {
		return type;
	}

	public double getConfidence() {
		return confidence;
	}

	public URI getDatatype() {
		return datatype;
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

	public String getSparql() {
		return sparql;
	}
}
