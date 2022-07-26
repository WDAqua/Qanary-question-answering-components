package eu.wdaqua.qanary.spotlightNED;

import com.google.gson.JsonArray;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.DBpediaSpotlightServiceNotAvailable;

@SpringBootApplication
@EnableAutoConfiguration
@EnableCaching
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

    /**
     * default main
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public DBpediaSpotlightConfiguration myDBpediaSpotlightConfiguration( //
                                                                          @Value("${dbpediaspotlight.test-question}") String testQuestion, //
                                                                          @Value("${dbpediaspotlight.confidence.minimum}") float confidenceMinimum, //
                                                                          @Value("${dbpediaspotlight.endpoint:https://api.dbpedia-spotlight.org/en/annotate}") String endpoint //
    ) throws DBpediaSpotlightServiceNotAvailable {
        this.checkSpotlightServiceAvailability(
                testQuestion, endpoint, confidenceMinimum, myDBpediaSpotlightServiceFetcher());
        return new DBpediaSpotlightConfiguration(confidenceMinimum, endpoint);
    }

    @Bean
    public QanaryComponent qanaryComponent() {
        return new DBpediaSpotlightNED();
    }

    @Bean
    public DBpediaSpotlightServiceFetcher myDBpediaSpotlightServiceFetcher() {
        return new DBpediaSpotlightServiceFetcher();
    }

    private void checkSpotlightServiceAvailability(
            String testQuestion, String endpoint, float confidenceMinimum,
            DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher) throws DBpediaSpotlightServiceNotAvailable {
        String err;
        try {
            JsonArray response = dBpediaSpotlightServiceFetcher.getJsonFromService(
                    testQuestion, endpoint, confidenceMinimum);
            return;
        } catch (Exception e) {
            err = e.getLocalizedMessage();
        }
        throw new DBpediaSpotlightServiceNotAvailable("No response from endpoint " + endpoint + "!\n" + err);
    }
}
