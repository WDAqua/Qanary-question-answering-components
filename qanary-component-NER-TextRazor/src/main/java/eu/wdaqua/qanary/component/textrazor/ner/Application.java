package eu.wdaqua.qanary.component.textrazor.ner;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.textrazor.ner.exception.ApiLiveTestFaildException;
import eu.wdaqua.qanary.component.textrazor.ner.exception.ApiTokenIsNullOrEmptyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

    RestTemplate myRestTemplate;
    CacheOfRestTemplateResponse myCacheOfResponses;

    Application(
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponses //
    ) {
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponses = myCacheOfResponses;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * this method is needed to make the QanaryComponent in this project known
     * to the QanaryServiceController in the qanary_component-template
     *
     * @return
     */
    @Bean
    public QanaryComponent qanaryComponent(
            @Value("${spring.application.name}") final String applicationName, //
            @Value("${textrazor.api.live.test.active}") final boolean apiLiveTestActive, //
            @Value("${textrazor.api.url}") final String url, //
            @Value("${textrazor.api.key}") final String key, //
            RestTemplate myRestTemplate, //
            CacheOfRestTemplateResponse myCacheOfResponses //
    ) throws ApiLiveTestFaildException, ApiTokenIsNullOrEmptyException {
        return new TextRazor(applicationName, apiLiveTestActive, url, key, myRestTemplate, myCacheOfResponses);
    }
}
