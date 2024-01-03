package eu.wdaqua.qanary.component.ned.gpt.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import eu.wdaqua.qanary.component.ned.gpt.GptBasedNamedEntityDisambiguation;
import eu.wdaqua.qanary.component.ned.gpt.NamedEntity;
import eu.wdaqua.qanary.component.ned.gpt.openai.OpenAiApiFetchingServiceFailedException;
import eu.wdaqua.qanary.component.ned.gpt.openai.OpenAiApiService;
import io.swagger.v3.oas.annotations.Operation;

/**
 * a controller for accessing the components functionality as a web service
 */
@RestController
@CrossOrigin
public class GptBasedNamedEntityDisambiguationController {
	public static final String ENDPOINT = "/gptbasedned";
	private static final Logger LOGGER = LoggerFactory.getLogger(GptBasedNamedEntityDisambiguationController.class);
	private final GptBasedNamedEntityDisambiguation myGptBasedNamedEntityDisambiguation;

	public GptBasedNamedEntityDisambiguationController(ConfigurableApplicationContext applicationContext,
			@Value("${spring.application.name}") final String applicationName, //
			@Value("${server.port}") int port, //
			GptBasedNamedEntityDisambiguation myGptBasedNamedEntityDisambiguation //
	) throws UnknownHostException {
		this.myGptBasedNamedEntityDisambiguation = myGptBasedNamedEntityDisambiguation;
		LOGGER.info("Web service {} is online at {}:{}", applicationName, InetAddress.getLocalHost().getHostAddress(),
				port);
	}

	@Operation(description = "Use the ChatGPT completions API (https://platform.openai.com/docs/api-reference/completions) to perform a Named Entity Recognition and Disambiguation task. You will receive an array with the results including Wikipedia URLs.")
	@GetMapping(value = ENDPOINT + "_with_wikipedia_urls", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonArray> getNamedEntitiesForShortTextsWikipedia(@RequestParam String sentence,
			@RequestParam String model) {
		LOGGER.info("GET request: {} on model {}", sentence, model);
		try {
			model = model.trim(); // safety
			return new ResponseEntity<JsonArray>(
					this.myGptBasedNamedEntityDisambiguation.getCompletion(sentence, model), HttpStatus.OK);
		} catch (OpenAiApiFetchingServiceFailedException e) {
			JsonArray returnValue = new JsonArray();
			returnValue.add(e.getMessage());
			return new ResponseEntity<JsonArray>(returnValue, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(description = "Use the ChatGPT completions API (https://platform.openai.com/docs/api-reference/completions) to perform a Named Entity Recognition and Disambiguation task. You will receive an array with the results including DBpedia URIs.")
	@GetMapping(value = ENDPOINT + "_with_dbpedia_urls", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonArray> getNamedEntitiesForShortTextsDBpedia(@RequestParam String sentence,
			@RequestParam String model) throws IOException {
		LOGGER.info("GET request: {} on model {}", sentence, model);
		try {
			model = model.trim(); // safety
			List<NamedEntity> resultsWikipedia = this.myGptBasedNamedEntityDisambiguation
					.getNamedEntitiesFromCompletion(sentence, model);
			List<NamedEntity> resultsDBpedia = this.myGptBasedNamedEntityDisambiguation
					.getDBpediaResourcesForNamedEntities(resultsWikipedia);
			JsonArray returnValues = new JsonArray();
			for (NamedEntity result : resultsDBpedia) {
				JsonObject myNamedEntity = new JsonObject();
				myNamedEntity.addProperty("start", result.getStartAsString());
				myNamedEntity.addProperty("end", result.getEndAsString());
				myNamedEntity.addProperty("string", result.getString());
				myNamedEntity.addProperty("resource", result.getResource().toASCIIString());
				returnValues.add(myNamedEntity);
			}
			return new ResponseEntity<JsonArray>(returnValues, HttpStatus.OK);
		} catch (OpenAiApiFetchingServiceFailedException e) {
			JsonArray returnValue = new JsonArray();
			returnValue.add(e.getMessage());
			return new ResponseEntity<JsonArray>(returnValue, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(description = "Get number of actually executed (not cached) requests to the Open AI API since the component was started.")
	@GetMapping(value = ENDPOINT + "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public JsonObject getNumberOfExecutedApiRequests() {
		JsonObject message = new JsonObject();
		message.addProperty("number_of_executed_not_cached_queries", OpenAiApiService.getNumberOfExecutedRequests());
		return message;
	}

}
