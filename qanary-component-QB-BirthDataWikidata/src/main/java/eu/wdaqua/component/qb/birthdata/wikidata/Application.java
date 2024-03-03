package eu.wdaqua.component.qb.birthdata.wikidata;

import eu.wdaqua.component.qb.birthdata.wikidata.web.BirthDataQueryBuilderController;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "eu.wdaqua.qanary" })
/**
 * basic class for wrapping functionality to a Qanary component note: there is
 * no need to change something here
 */
public class Application {

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * this method is needed to make the QanaryComponent in this project known to
	 * the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 */
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName) {
		return new BirthDataQueryBuilder(applicationName);
	}

	@Bean
	public BirthDataQueryBuilderController getBirthDataQueryBuilderController(
			BirthDataQueryBuilder myBirthDataQueryBuilder) {
		return new BirthDataQueryBuilderController(myBirthDataQueryBuilder);
	}

	@Bean
	public OpenAPI customOpenAPI() {
		String appVersion = getClass().getPackage().getImplementationVersion();
		return new OpenAPI().info(new Info() //
				.title("Qanary Wikidata Birth Data Query Builder Component") //
				.version(appVersion) //
				.description("This is a Qanary component for creating Wikidata queries " //
						+ "to find the birth place and date of named entities in text questions.")
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
