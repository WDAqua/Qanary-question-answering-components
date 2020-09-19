package eu.wdaqua.qanary.tagme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@ComponentScan("eu.wdaqua.qanary.component")
/**
 * basic class for wrapping functionality to a Qanary component note: there is
 * no need to change something here
 */
public class Application {

	/**
	 * this method is needed to make the QanaryComponent in this project known to
	 * the QanaryServiceController in the qanary_component-template
	 * 
	 * @return
	 */
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName,
			@Value("${ned-tagme.cache.enabled}") final Boolean cacheEnabled,
			@Value("${ned-tagme.cache.file}") final String cacheFile,
			@Value("${ned-tagme.service.url}") final String tagMeServiceURL,
			@Value("${ned-tagme.link_propability.threshold:0.25}") final float tagMeMinimumLinkPropability) {
		return new TagmeNED(applicationName, cacheEnabled, cacheFile, tagMeServiceURL, tagMeMinimumLinkPropability);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
