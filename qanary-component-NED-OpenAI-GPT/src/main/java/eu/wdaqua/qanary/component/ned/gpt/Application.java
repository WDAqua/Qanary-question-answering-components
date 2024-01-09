package eu.wdaqua.qanary.component.ned.gpt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = { "eu.wdaqua.qanary" })
@EnableCaching
/**
 * basic class for wrapping functionality to a Qanary component note: there is
 * no need to change something here
 */
public class Application {
	@SuppressWarnings("unused")
	private static ApplicationContext applicationContext;

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;

	public static void main(String[] args) {
		applicationContext = SpringApplication.run(Application.class, args);
	}

	@Bean
	public OpenAPI customOpenAPI( //
			@Value("${springdoc.version}") String appVersion, //
			@Value("${spring.application.name}") String appName //
	) {
		return new OpenAPI().info(new Info() //
				.title(appName) //
				.version(appVersion) //
				.description(
						"OpenAPI 3 with Spring Boot provided this API documentation. It uses the current component's settings." //
				) //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
