package eu.wdaqua.qanary.component.platypuswrapper.qb;

import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.DataNotProcessableException;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.PlatypusRequest;
import eu.wdaqua.qanary.component.platypuswrapper.qb.messages.PlatypusResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class PlatypusQueryBuilderController {
    private static final Logger logger = LoggerFactory.getLogger(PlatypusQueryBuilderController.class);
    private PlatypusQueryBuilder platypusQueryBuilder;

    private URI endpoint;
    private String langFallback;
    
    public PlatypusQueryBuilderController(
            PlatypusQueryBuilder platypusQueryBuilder, 
            @Qualifier("platypus.endpointUrl") URI endpoint,
            @Qualifier("platypus.langDefault") String langDefault,
            @Value("${server.port}") String serverPort, 
            @Value("${springdoc.api-docs.path}") String swaggerApiDocsPath, 
            @Value("${springdoc.swagger-ui.path}") String swaggerUiPath 
    ) {
        this.platypusQueryBuilder = platypusQueryBuilder;
        this.endpoint = endpoint;
        this.langFallback = langDefault;

        logger.info("Service API docs available at http://0.0.0.0:{}{}", serverPort, swaggerApiDocsPath);
        logger.info("Service API docs UI available at http://0.0.0.0:{}{}", serverPort, swaggerUiPath);
    }

    @PostMapping(value = "/api", produces = "application/json")
    @ResponseBody
    public PlatypusResult requestPlatypusWebService(@RequestBody PlatypusRequest request) throws URISyntaxException, DataNotProcessableException {
        logger.info("requestPlatypusWebService: {}", request);
        request.replaceNullValuesWithDefaultValues(this.getEndpoint(), this.getLangFallback());
        return platypusQueryBuilder.requestPlatypusWebService(request);
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getLangFallback() {
        return langFallback;
    }

}
