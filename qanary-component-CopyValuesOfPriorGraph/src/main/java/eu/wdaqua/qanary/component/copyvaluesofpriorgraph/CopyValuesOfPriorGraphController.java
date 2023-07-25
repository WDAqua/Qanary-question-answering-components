package eu.wdaqua.qanary.component.copyvaluesofpriorgraph;

import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

@Controller
public class CopyValuesOfPriorGraphController {
	private static final Logger logger = LoggerFactory.getLogger(CopyValuesOfPriorGraphController.class);
	private CopyValuesOfPriorGraph copyValuesOfPriorGraph;

	public CopyValuesOfPriorGraphController( //
			CopyValuesOfPriorGraph copyValuesOfPriorGraph, //
			@Value("${server.port}") String serverPort, //
			@Value("${springdoc.api-docs.path}") String swaggerApiDocsPath, //
			@Value("${springdoc.swagger-ui.path}") String swaggerUiPath //
	) {
		this.copyValuesOfPriorGraph = copyValuesOfPriorGraph;
		logger.info("Service API docs available at http://0.0.0.0:{}{}", serverPort, swaggerApiDocsPath);
		logger.info("Service API docs UI available at http://0.0.0.0:{}{}", serverPort, swaggerUiPath);
	} 

	@PostMapping(value = "/copyvaluestograph") 
	@ResponseStatus(value = HttpStatus.OK)
	public void copyValuesToGraph( 
			@RequestParam String sourceGraph, 
			@RequestParam String targetGraph
	) throws Exception {
		this.copyValuesOfPriorGraph.addDataToGraph(sourceGraph, targetGraph);
	}

}
