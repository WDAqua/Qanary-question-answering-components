package eu.wdaqua.qanary.sparqlexecuter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SpringBootApplication
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
	public SparqlExecuter qanaryComponent(@Value("${spring.application.name}") final String applicationName) {
		return new SparqlExecuter(applicationName);
	}

	/**
	 * the version is taken from the JAR manifest, so the running component reports the
	 * version it was built from -- this is what makes the deployed version verifiable
	 * (see service_config/verify_deployment.py)
	 */
	@Bean
	public OpenAPI customOpenAPI() {
		String appVersion = getClass().getPackage().getImplementationVersion();
		return new OpenAPI().info(new Info() //
				.title("Qanary SPARQL Executer Component") //
				.version(appVersion) //
				.description("Executes the SPARQL query computed by a previous component " //
						+ "automatically on Wikidata or DBpedia") //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
	
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
