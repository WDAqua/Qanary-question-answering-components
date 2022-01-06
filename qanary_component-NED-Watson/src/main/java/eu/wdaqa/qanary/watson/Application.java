package eu.wdaqa.qanary.watson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.net.URI;

import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@ComponentScan("eu.wdaqua.qanary.component")
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
	public WatsonNED qanaryComponent(
			@Value("${spring.application.name}") final String applicationName,
			@Value("${ned-watson.cache.enabled}") final boolean cacheEnabled,
			@Value("${ned-watson.cache.file}") final String cacheFile,
			@Value("${ned-watson.service.url}") final URI watsonServiceURL,
			@Value("${ned-watson.service.key}") final String watsonServiceKey
	) {
		return new WatsonNED(applicationName, cacheEnabled, cacheFile, watsonServiceURL, watsonServiceKey);
	}
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
