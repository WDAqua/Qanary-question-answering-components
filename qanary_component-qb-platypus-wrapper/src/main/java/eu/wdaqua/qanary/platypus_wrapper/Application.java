package eu.wdaqua.qanary.platypus_wrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/

	@Bean(name = "langDefault")
	String langDefault(@Value("${platypus.endpoint.language.default:en}") String langDefault) {
		return langDefault;
	}

	@Bean(name = "endpointUrl")
	URI endpointUrl(@Value("${platypus.endpoint.url}") String endpointUrl) throws URISyntaxException {
		return new URI(endpointUrl);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Bean
	public QanaryComponent qanaryComponent( //
			@Qualifier("langDefault") String langDefault, //
			@Qualifier("endpointUrl") URI endpoint, //
			@Value("${spring.application.name}") final String applicationName, //
			RestTemplate restTemplate //
	) throws URISyntaxException {
		return new PlatypusQueryBuilder(langDefault, endpoint, applicationName, restTemplate);
	}

	/*
	@Bean
	public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
		return new OpenAPI().info(new Info() //
				.title("SimpleSpringService Data API") //
				.version(appVersion) //
				.description("This is a sample Foobar server created using springdocs - "
						+ "a library for OpenAPI 3 with spring boot.")
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
	*/

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}