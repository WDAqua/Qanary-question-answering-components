package eu.wdaqua.qanary.component.deeppavlovwrapper.qb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
@EnableAutoConfiguration
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

	@Autowired
	CacheOfRestTemplateResponse myCacheOfResponses;

	@Bean(name = "deeppavlov.langDefault")
	String langDefault(@Value("${deeppavlov.endpoint.language.default:en}") String langDefault) {
			return langDefault;
	}

	@Bean(name = "deeppavlov.endpoint.language.supported")
	ArrayList<String> supportedLang(@Value("${deeppavlov.endpoint.language.supported:en}") ArrayList<String> supportedLang) {
			return supportedLang;
	}

	@Bean(name = "deeppavlov.endpointUrl")
	URI endpointUrl(@Value("${deeppavlov.endpoint.url}") String endpointUrl) throws URISyntaxException {
			return new URI(endpointUrl);
	}

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent(
			@Value("${spring.application.name}") final String applicationName,
			@Qualifier("deeppavlov.langDefault") String langDefault,
			@Qualifier("deeppavlov.endpoint.language.supported") ArrayList<String> supportedLang, 
			@Qualifier("deeppavlov.endpointUrl") URI endpoint,
			RestTemplateWithCaching restTemplate) {
		return new DeepPavlovWrapper(
				applicationName,
				langDefault,
				supportedLang,
				endpoint,
				restTemplate,
				myCacheOfResponses
				);
	}

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;
	
  public static void main(String[] args) {
      SpringApplication.run(Application.class, args);
  }

  @Bean
  public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
      return new OpenAPI().info(new Info() //
              .title("DeepPavlov wrapper component") //
              .version(appVersion) //
              .description("This is a sample Foobar server created using springdocs - " + "a library for OpenAPI 3 with spring boot.").termsOfService("http://swagger.io/terms/") //
              .license(new License().name("Apache 2.0").url("http://springdoc.org")) //
      );
  }
}
