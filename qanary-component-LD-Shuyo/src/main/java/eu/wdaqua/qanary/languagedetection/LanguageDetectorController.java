package eu.wdaqua.qanary.languagedetection;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.languagedetection.messages.LanguageDetectionRequest;
import eu.wdaqua.qanary.languagedetection.messages.LanguageDetectionResponse;
import io.swagger.v3.oas.annotations.Operation;

@Controller
public class LanguageDetectorController {
	private static final Logger logger = LoggerFactory.getLogger(LanguageDetectorController.class);
	private LanguageDetection myLanguageDetection;

	public LanguageDetectorController( //
			LanguageDetection myLanguageDetection, //
			@Value("${server.port}") String serverPort, //
			@Value("${springdoc.api-docs.path}") String swaggerApiDocsPath, //
			@Value("${springdoc.swagger-ui.path}") String swaggerUiPath //
	) throws URISyntaxException {
		this.myLanguageDetection = myLanguageDetection;
		logger.info("Service API docs available at http://0.0.0.0:{}{}", serverPort, swaggerApiDocsPath);
		logger.info("Service API docs UI available at http://0.0.0.0:{}{}", serverPort, swaggerUiPath);
	}

	/**
	 * simple controller receiving text and response with a list of language codes
	 * (typically it would be one) be aware that this function might return a
	 * language array containing one value that has the value 'null'
	 * 
	 * example:
	 * 
	 * <pre>
			curl -X POST "http://localhost:5555/api" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{\"text\":\"What is the capital of Germany?\"}"
	
			{"text":"What is the capital of Germany?","languages":["en"]}
	 * </pre>
	 * 
	 * example:
	 * 
	 * <pre>
			curl -X POST "http://localhost:5555/api" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{\"text\":\"12345\"}"
	
			{"text":"12345","languages":[null]}
	 * </pre>
	 * 
	 * 
	 * @param myLanguageDetectionRequest
	 * @return
	 * @throws LangDetectException
	 */
	@PostMapping(value = "/api", produces = "application/json")
	@ResponseBody
	@Operation(summary = "Test Web interface to send a request containing text, the text is used to determine the language", //
			operationId = "requestLanguageDetectionWebService", //
			description = "Only the text parameter is required. " //
					+ "Examples: \"What is the capital of Germany?\",  " //
					+ "\"What is the capital of http://www.wikidata.org/entity/Q183?\", " //
					+ "\"Wie viele Personen leben in Frankreich?\", " //
					+ "\"Ist Berlin die Hauptstadt von Deutschland\" " //
	)
	public HttpEntity<LanguageDetectionResponse> getLanguageOfText(
			@RequestBody LanguageDetectionRequest myLanguageDetectionRequest) throws LangDetectException {
		LanguageDetectionResponse result = new LanguageDetectionResponse(this.myLanguageDetection,
				myLanguageDetectionRequest.getText());
		logger.info("LanguageDetectionResponse: {}", result.toString());
		return new HttpEntity<LanguageDetectionResponse>(result);
	}
}
