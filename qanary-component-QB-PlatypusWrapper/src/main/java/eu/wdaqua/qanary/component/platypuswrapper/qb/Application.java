package eu.wdaqua.qanary.component.platypuswrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component note: there is
 * no need to change something here
 */ public class Application {

    private static ApplicationContext applicationContext;

    /**
     * this method is needed to make the QanaryComponent in this project known
     * to the QanaryServiceController in the qanary_component-template
     *
     * @return
     */

    @Bean
    float threshold(@Value("${platypus.threshold:0.5}") float threshold) {
        return threshold;
    }

    @Bean(name = "platypus.langDefault")
    String langDefault(@Value("${platypus.endpoint.language.default:en}") String langDefault) {
        return langDefault;
    }

    @Autowired
    public QanaryComponentConfiguration qanaryComponentConfiguration;

    @Bean(name = "platypus.endpointUrl")
    URI endpointUrl(@Value("${platypus.endpoint.url}") String endpointUrl) throws URISyntaxException {
        return new URI(endpointUrl);
    }

    @Autowired
    CacheOfRestTemplateResponse myCacheOfResponses;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(Application.class, args);
    }

    @Bean(name = "platypus.endpoint.language.supported")
    ArrayList<String> supportedLang(@Value("${platypus.endpoint.language.supported:en}") ArrayList<String> supportedLang) {
        return supportedLang;
    }

    @Bean
    public QanaryComponent qanaryComponent( //
                                            float threshold, //
                                            @Qualifier("platypus.langDefault") String langDefault, //
                                            @Qualifier("platypus.endpoint.language.supported") List<String> supportedLang, //
                                            @Qualifier("platypus.endpointUrl") URI endpoint, //
                                            @Value("${spring.application.name}") final String applicationName, //
                                            RestTemplateWithCaching restTemplate //
    ) throws URISyntaxException {
        return new PlatypusQueryBuilder(threshold, langDefault, supportedLang, endpoint, applicationName, restTemplate, myCacheOfResponses);
    }

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version:none}") final String springdocVersion) {
		    String appVersion = getClass().getPackage().getImplementationVersion();
        return new OpenAPI().info(new Info() //
                .title("Platypus wrapper component") //
                .version(springdocVersion) //
                .description("This is a server created using springdocs " +  springdocVersion + " - a library for OpenAPI 3 with Spring Boot.") //
                .termsOfService("http://swagger.io/terms/") //
                .license(new License().name("Apache 2.0").url("http://springdoc.org")) //
        );
    }

}
