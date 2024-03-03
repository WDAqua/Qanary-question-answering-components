package eu.wdaqua.qanary.component.copyvaluesofpriorgraph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
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
	@Bean
	public QanaryComponent qanaryComponent(
			@Value("${spring.application.name}") final String applicationName,
			@Value("${spring.boot.admin.url}") final String adminUrl,
			RestTemplate restTemplate) {
		return new CopyValuesOfPriorGraph(applicationName, adminUrl, restTemplate);
	}

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;

	public static void main(String[] args) { 
		SpringApplication.run(Application.class, args); 
	}

	@Bean
	public OpenAPI customOpenAPI() {
		String appVersion = getClass().getPackage().getImplementationVersion();
		return new OpenAPI().info(new Info() //
				.title("component CopyValuesOfPriorGraph") //
				.version(appVersion) //
				.description("This is a sample Foobar server created using springdocs - "
						+ "a library for OpenAPI 3 with spring boot.")
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
