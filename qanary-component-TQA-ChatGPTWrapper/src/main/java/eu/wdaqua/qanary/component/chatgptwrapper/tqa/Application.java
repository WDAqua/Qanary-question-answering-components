package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.apache.commons.cli.MissingArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.net.URISyntaxException;

@SpringBootApplication
@ComponentScan(basePackages = { "eu.wdaqua.qanary" })
/**
 * basic class for wrapping functionality to a Qanary component note: there is
 * no need to change something here
 */
public class Application {
	private static ApplicationContext applicationContext;
	private CacheOfRestTemplateResponse myCacheOfResponses;
	@Autowired
	private Environment env;

	public Application(@Autowired CacheOfRestTemplateResponse myCacheOfResponses) {
		this.myCacheOfResponses = myCacheOfResponses;
	}

	public static void main(String[] args) {
		applicationContext = SpringApplication.run(Application.class, args);
	}

	/**
	 * this method is needed to make the QanaryComponent in this project known to
	 * the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 * @throws MissingArgumentException
	 */
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") String applicationName, //
										   @Value("${chatgpt.api.key}") String token, //
										   @Value("${chatgpt.api.live.test.active}") boolean doApiIsAliveCheck, //
										   @Value("${chatgpt.model}") String model, //
										   @Value("${chatgpt.base.url}") String baseUrl, //
										   RestTemplateWithCaching restTemplateWithCaching)
			throws MissingTokenException, URISyntaxException, OpenApiUnreachableException, MissingArgumentException {
		return new ChatGPTWrapper(applicationName, token, doApiIsAliveCheck, model, baseUrl, restTemplateWithCaching,
				myCacheOfResponses);
	}

	@Bean
	public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
		return new OpenAPI().info(new Info() //
				.title("Qanry ChatGPT wrapper component") //
				.version(appVersion) //
				.description(
						"OpenAPI 3 with Spring Boot provided this API documentation. It uses the current component's settings:<ul>" //
								+ "  <li>chatgpt.model: " + env.getProperty("chatgpt.model") + "</li>" //
								+ "  <li>chatgpt.base.url: " + env.getProperty("chatgpt.base.url") + "</li>" //
								+ "</ul>") //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
