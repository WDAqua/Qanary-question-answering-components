package eu.wdaqua.qanary.component.qanswer.qbe;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.component.qanswer.qbe.messages.NoLiteralFieldFoundException;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.QAnswerRequest;
import eu.wdaqua.qanary.component.qanswer.qbe.messages.QAnswerResult;
import io.swagger.v3.oas.annotations.Operation;

@Controller
public class QAnswerQueryBuilderAndExecutorController {
	private static final Logger logger = LoggerFactory.getLogger(QAnswerQueryBuilderAndExecutorController.class);
	private QAnswerQueryBuilderAndExecutor myQAnswerQueryBuilderAndExecutor;

	private URI endpoint;
	private String langFallback;
	private String knowledgeBaseDefault;

	public QAnswerQueryBuilderAndExecutorController( //
			QAnswerQueryBuilderAndExecutor myQAnswerQueryBuilderAndExecutor, //
			@Qualifier("langDefault") String langDefault, //
			@Qualifier("knowledgeBaseDefault") String knowledgeBaseDefault, //
			@Qualifier("endpointUrl") URI endpoint, //
			@Value("${server.port}") String serverPort, //
			@Value("${springdoc.api-docs.path}") String swaggerApiDocsPath, //
			@Value("${springdoc.swagger-ui.path}") String swaggerUiPath //
	) throws URISyntaxException {
		this.myQAnswerQueryBuilderAndExecutor = myQAnswerQueryBuilderAndExecutor;
		this.endpoint = endpoint;
		this.langFallback = langDefault;
		this.knowledgeBaseDefault = knowledgeBaseDefault;

		logger.info("Service API docs available at http://0.0.0.0:{}{}", serverPort, swaggerApiDocsPath);
		logger.info("Service API docs UI available at http://0.0.0.0:{}{}", serverPort, swaggerUiPath);
	}

	/**
	 * POST interface for requesting the data
	 * 
	 * <pre>
		 {
	    	"qanswerEndpointUrl": "http://qanswer-core1.univ-st-etienne.fr/api/gerbil",
	    	"questionString": "What is the capital of Germany?",
	    	"lang": "en",
	    	"knowledgeBaseId": "wikidata"
		 }
	 * </pre>
	 * 
	 * OR
	 * 
	 * <pre>
		{
		    "question": "What is the capital of Germany?"
		}
	 * </pre>
	 * 
	 * OR
	 * 
	 * <pre>
		{
		    "question": "population of http://www.wikidata.org/entity/Q142"
		}
	 * </pre>
	 * 
	 * @param request
	 * @return
	 * @throws URISyntaxException
	 * @throws NoLiteralFieldFoundException
	 */

	@PostMapping(value = "/api", produces = "application/json")
	@ResponseBody
	@Operation(summary = "Send a request to the QAnswer API", //
			operationId = "requestQAnswerWebService", //
			description = "Only the question parameter is required. " //
					+ "Examples: \"What is the capital of Germany?\",  " //
					+ "\"What is the capital of http://www.wikidata.org/entity/Q183?\", " //
					+ "\"Person born in France\", " //
					+ "\"Person born in http://www.wikidata.org/entity/Q142?\", " //
					+ "\"Is Berlin the capital of Germany\" " //
	)
	public QAnswerResult requestQAnswerWebService(@RequestBody QAnswerRequest request)
			throws URISyntaxException, NoLiteralFieldFoundException {
		logger.info("requestQAnswerWebService: {} ", request);
		request.replaceNullValuesWithDefaultValues(this.getEndpoint(), this.getLangFallback(),
				this.getKnowledgeBaseDefault());
		return myQAnswerQueryBuilderAndExecutor.requestQAnswerWebService(request);
	}

	public URI getEndpoint() {
		return endpoint;
	}

	public String getLangFallback() {
		return langFallback;
	}

	public String getKnowledgeBaseDefault() {
		return knowledgeBaseDefault;
	}

}
