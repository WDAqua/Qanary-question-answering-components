package eu.wdaqua.qanary.tebaqa.wrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
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

	private static ApplicationContext applicationContext;

	/**
	 * this method is needed to make the QanaryComponent in this project known to
	 * the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 */

	@Bean
	float threshold(@Value("${tebaqa.threshold:0.5}") float threshold) {
		return threshold;
	}

	@Bean(name = "tebaqa.langDefault")
	String langDefault(@Value("${tebaqa.endpoint.language.default:en}") String langDefault) {
		return langDefault;
	}

	@Autowired
	CacheOfRestTemplateResponse myCacheOfResponses;

	@Bean(name = "tebaqa.endpointUrl")
	URI endpointUrl(@Value("${tebaqa.endpoint.url}") String endpointUrl) throws URISyntaxException {
		return new URI(endpointUrl);
	}

	public static void main(String[] args) {
		applicationContext = SpringApplication.run(Application.class, args);
	}

	@Bean(name = "tebaqa.endpoint.language.supported")
	ArrayList<String> supportedLang(
			@Value("${tebaqa.endpoint.language.supported:en}") ArrayList<String> supportedLang) {
		return supportedLang;
	}

	@Bean
	public QanaryComponent qanaryComponent( //
			float threshold, //
			@Qualifier("tebaqa.langDefault") String langDefault, //
			@Qualifier("tebaqa.endpoint.language.supported") ArrayList<String> supportedLang, //
			@Qualifier("tebaqa.endpointUrl") URI endpoint, //
			@Value("${spring.application.name}") final String applicationName, //
			RestTemplateWithCaching restTemplate //
	) throws URISyntaxException {
		return new TeBaQAQueryBuilder(threshold, langDefault, supportedLang, endpoint, applicationName, restTemplate,
				myCacheOfResponses);
	}

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;

	@Bean
	public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
		return new OpenAPI().info(new Info() //
				.title("TeBaQa wrapper component") //
				.version(appVersion) //
				.description("This is a sample Foobar server created using springdocs - "
						+ "a library for OpenAPI 3 with spring boot.")
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}

}
