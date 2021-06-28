package eu.wdaqua.opentapiocaNED;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

	@Bean
	public OpenTapiocaConfiguration openTapiocaConfiguration( //
			@Value("${opentapioca.endpoint:https://opentaiopca.org/api}") String endpoint
	) {
			return new OpenTapiocaConfiguration(endpoint);
	}

	@Bean
	public OpenTapiocaServiceFetcher openTapiocaServiceFetcher() {
		return new OpenTapiocaServiceFetcher();
	}

	@Bean
	public QanaryComponent qanaryComponent (
			@Value("${spring.application.name}") final String applicationName,
			OpenTapiocaConfiguration openTapiocaConfiguration, 
			OpenTapiocaServiceFetcher openTapiocaServiceFetcher)

	{
		return new OpenTapiocaNED(
				applicationName,
				openTapiocaConfiguration,
				openTapiocaServiceFetcher);
	}
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

	@Bean
	public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
		return new OpenAPI().info(new Info() //
				.title("Qanary OpenTapioca Component") //
				.version(appVersion) //
				.description("This is a Qanary component for identifying Wikidata resources " //
					+ "in text questions using the OpenTapioca endpoint available at " //
					+ "http://opentapioca.org") //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
