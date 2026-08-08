package eu.wdaqua.qanary.component.comiccharacternamesimple.ner;

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

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
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
				.title("Qanary Comic Character Name Simple Named Entity Recognizer Component") //
				.version(appVersion) //
				.description("Annotates DBpedia superhero entities in a question, e.g. \"Catwoman\" " //
						+ "in \"What is the real name of Catwoman?\"") //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}

	/**
	 * this method is needed to make the QanaryComponent in this project known
	 * to the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 */
	@Bean
	public ComicCharacterNameSimpleNamedEntityRecognizer qanaryComponent(@Value("${spring.application.name}") String applicationName) {
		return new ComicCharacterNameSimpleNamedEntityRecognizer(applicationName);
	}
}
