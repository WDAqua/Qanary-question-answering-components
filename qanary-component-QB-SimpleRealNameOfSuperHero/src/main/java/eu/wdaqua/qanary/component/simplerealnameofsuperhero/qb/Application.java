
package eu.wdaqua.qanary.component.simplerealnameofsuperhero.qb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SpringBootApplication
@ComponentScan(basePackages = { "eu.wdaqua.qanary" })
/**
 * basic class for wrapping functionality to a Qanary component note: there is
 * no need to change something here
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
				.title("Qanary Simple Real Name Of Super Hero Query Builder Component") //
				.version(appVersion) //
				.description("Builds a DBpedia query retrieving the real name of a superhero " //
						+ "character identified by a previous component") //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}

}
