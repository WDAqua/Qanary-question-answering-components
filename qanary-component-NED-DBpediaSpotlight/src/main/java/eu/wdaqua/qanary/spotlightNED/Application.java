package eu.wdaqua.qanary.spotlightNED;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.DBpediaSpotlightServiceNotAvailable;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
public class Application {
    private static ApplicationContext applicationContext;
    @Autowired
    CacheOfRestTemplateResponse myCacheOfResponses;
    @Autowired
    RestTemplate restTemplate;

    /**
     * default main
     */
    public static void main(String[] args) {
        applicationContext = SpringApplication.run(Application.class, args);
    }

    @Bean
    public DBpediaSpotlightConfiguration myDBpediaSpotlightConfiguration(//
                                                                         @Value("${dbpediaspotlight.test-question}") String testQuestion, //
                                                                         @Value("${dbpediaspotlight.confidence.minimum}") float confidenceMinimum, //
                                                                         @Value("${dbpediaspotlight.endpoint:https://api.dbpedia-spotlight.org/en/annotate}") String endpoint //
    ) throws DBpediaSpotlightServiceNotAvailable {
        this.checkSpotlightServiceAvailability(testQuestion, endpoint, confidenceMinimum, myDBpediaSpotlightServiceFetcher());
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

    private void checkSpotlightServiceAvailability(String testQuestion, String endpoint, float confidenceMinimum, DBpediaSpotlightServiceFetcher dBpediaSpotlightServiceFetcher) throws DBpediaSpotlightServiceNotAvailable {
        String err;
        try {
            JsonArray response = dBpediaSpotlightServiceFetcher.getJsonFromService(testQuestion, endpoint, confidenceMinimum, restTemplate, myCacheOfResponses);
            return;
        } catch (Exception e) {
            err = e.getLocalizedMessage();
        }
        throw new DBpediaSpotlightServiceNotAvailable("No response from endpoint " + endpoint + "!\n" + err);
    }

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        return new OpenAPI().info(new Info() //
                .title("NED DBpediaSpotlight component") //
                .version(appVersion) //
                .description("This is a sample Foobar server created using springdocs - " + "a library for OpenAPI 3 with spring boot.").termsOfService("http://swagger.io/terms/") //
                .license(new License().name("Apache 2.0").url("http://springdoc.org")) //
        );
    }
}
