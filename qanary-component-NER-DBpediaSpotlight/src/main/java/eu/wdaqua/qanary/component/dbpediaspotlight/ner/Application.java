package eu.wdaqua.qanary.component.dbpediaspotlight.ner;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.dbpediaspotlight.ner.exceptions.DBpediaSpotlightServiceNotAvailable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

/**
 * Created by didier on 27.03.16.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * this method is needed to make the QanaryComponent in this project known to
     * the QanaryServiceController in the qanary_component-template
     */
    @Bean
    public QanaryComponent qanaryComponent(
            @Value("${spring.application.name}") final String applicationName, //
            DBpediaSpotlightConfiguration dBpediaSpotlightConfiguration, //
            DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher, //
            @Autowired RestTemplate myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfRestTemplateResponse //
    ) {
        return new DBpediaSpotlightNER(
                applicationName, //
                dBpediaSpotlightConfiguration, //
                dBpediaSpotlightServiceFetcher, //
                myRestTemplate, //
                myCacheOfRestTemplateResponse //
        );
    }

    @Bean
    public DBpediaSpotlightConfiguration dBpediaSpotlightConfiguration(
            @Value("${dbpediaspotlight.test-question}") String testQuestion, //
            @Value("${dbpediaspotlight.confidence.minimum}") float confidenceMinimum, //
            @Value("${dbpediaspotlight.endpoint:https://api.dbpedia-spotlight.org/en/annotate}") String endpoint, //
            DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher, //
            @Value("${dbpediaspotlight.endpoint.check.availability:false}") boolean checkAvailability, //
            @Autowired RestTemplate myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfRestTemplateResponse //
    ) throws DBpediaSpotlightServiceNotAvailable {
        this.checkSpotlightServiceAvailability(
                testQuestion, //
                endpoint, //
                confidenceMinimum, //
                dBpediaSpotlightServiceFetcher, //
                checkAvailability, //
                myRestTemplate, //
                myCacheOfRestTemplateResponse //
        );
        LOGGER.debug("endpoint: {}", endpoint);
        return new DBpediaSpotlightConfiguration(confidenceMinimum, endpoint);
    }

    @Bean
    DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher() {
        return new DBpediaSpotlightServiceFetcher();
    }

    private void checkSpotlightServiceAvailability(
            String testQuestion,
            String endpoint,
            float confidenceMinimum,
            DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher,
            boolean checkAvailability,
            RestTemplate myRestTemplate, //
            CacheOfRestTemplateResponse myCacheOfRestTemplateResponse //
    ) throws DBpediaSpotlightServiceNotAvailable {
        if (checkAvailability == false) {
            LOGGER.warn("Availability check of DBpedia Spotlight service is disabled!");
            return;
        }

        String err;
        try {
            JsonArray response = dBpediaSpotlightServiceFetcher.getJsonFromService(
                    testQuestion, //
                    endpoint, //
                    confidenceMinimum, //
                    myRestTemplate, //
                    myCacheOfRestTemplateResponse //
            );
            return;
        } catch (Exception e) {
            err = e.getLocalizedMessage();
        }
        throw new DBpediaSpotlightServiceNotAvailable("No response from endpoint " + endpoint + "!\n" + err);
    }
}
