package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {
	private static ApplicationContext applicationContext;
	private CacheOfRestTemplateResponse myCacheOfResponses;

	public Application(
			@Autowired CacheOfRestTemplateResponse myCacheOfResponses
	) {
		this.myCacheOfResponses = myCacheOfResponses;
	}

	public static void main(String[] args) {
		applicationContext = SpringApplication.run(Application.class, args);
	}

	/**
	 * this method is needed to make the QanaryComponent in this project known
	 * to the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 */
	@Bean
	public QanaryComponent qanaryComponent(
			@Value("${spring.application.name}") String applicationName,
			@Value("${chatgpt.key}") String token,
			@Value("${chatgpt.api.live.test}") boolean doApiIsAliveCheck,
			@Value("${chatgpt.model}") String model,
			RestTemplateWithCaching restTemplateWithCaching
	) throws MissingTokenException, URISyntaxException, OpenApiUnreachableException {
		return new ChatGPTWrapper(applicationName, token, doApiIsAliveCheck, model, restTemplateWithCaching, myCacheOfResponses);
	}

	@Bean
	public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
		return new OpenAPI().info(new Info() //
				.title("ChatGPT wrapper component") //
				.version(appVersion) //
				.description("This is a sample Foobar server created using springdocs - " + "a library for OpenAPI 3 with spring boot.").termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
