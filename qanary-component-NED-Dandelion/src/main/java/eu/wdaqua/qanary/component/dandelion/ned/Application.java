package eu.wdaqua.qanary.component.dandelion.ned;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.dandelion.ned.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.dandelion.ned.exception.ApiTokenIsNullOrEmptyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {
    CacheOfRestTemplateResponse myCacheOfResponses;
    RestTemplateWithCaching myRestTemplate;

    public Application( //
                        @Autowired RestTemplateWithCaching myRestTemplate, //
                        @Autowired CacheOfRestTemplateResponse myCacheOfResponses //
    ) {
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
    }

    /**
     * this method is needed to make the QanaryComponent in this project known
     * to the QanaryServiceController in the qanary_component-template
     *
     * @return
     */
    @Bean
    public QanaryComponent qanaryComponent( //
                                            @Value("${spring.application.name}") final String applicationName, //
                                            @Value("${ned.dandelion.api.live.test.active}") final boolean apiLiveTestActive, //
                                            @Value("${ned.dandelion.api.token}") final String apiToken //
    ) throws ApiTokenIsNullOrEmptyException, ApiLiveTestFaildException {
        return new DandelionNED(applicationName, myRestTemplate, myCacheOfResponses, apiLiveTestActive, apiToken);
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
