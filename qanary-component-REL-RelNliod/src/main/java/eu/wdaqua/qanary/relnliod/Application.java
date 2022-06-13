package eu.wdaqua.qanary.relnliod;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@ComponentScan("eu.wdaqua.qanary.component")
@ComponentScan("eu.wdaqua.qanary.relnliod")
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
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName,
										   @Value("${rel-nliod.cache.enabled}") final Boolean cacheEnabled,
										   @Value("${rel-nliod.cache.file}") final String cacheFile,
										   @Value("${rel-nliod.service.url}") final String textRazorServiceURL,
										   @Value("${rel-nliod.service.key}") final String textRazorServiceKey,
										   final DbpediaRecordProperty dbpediaRecordProperty,
										   final RemovalList removalList) {
		return new RelNliodRel(applicationName, cacheEnabled, cacheFile, textRazorServiceURL, textRazorServiceKey,dbpediaRecordProperty, removalList);
	}
	
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}