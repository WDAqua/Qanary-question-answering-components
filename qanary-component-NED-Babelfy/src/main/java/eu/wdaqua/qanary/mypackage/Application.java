package eu.wdaqua.qanary.mypackage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.mypackage.BabelfyServiceFetcher.Link;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName) {
		return new BabelfyNED(applicationName);
	}

	@Bean
	public BabelfyConfiguration babelfyConfiguration(
			@Value("${service.endpoint}") final String endpoint,
			@Value("${service.test-question}") final String testQuetsion,
			@Value("${service.parameters}") final String parameters)
	throws Exception {
		// check functionality with live API
		for (int i = 0; i < 10; i++) {
			try {
				this.testFunctionality(endpoint, testQuetsion, parameters);
				logger.info("Functionality works as expectted");
				break;
			} catch (Exception e) {
				logger.warn("Functionality does not work as expected on attempt no. {}:{}", i, e.toString());
			}
			if (i > 8) {
				logger.error("Functionality does not work after maximum tries. Exiting...");
				throw new Exception("Could not start component");
			}
		}
		return new BabelfyConfiguration(endpoint, testQuetsion, parameters);
	}

	@Bean BabelfyServiceFetcher babelfyServiceFetcher() {
		return new BabelfyServiceFetcher();
	}

	// test the internal functionality of this component, using live APIs
	private void testFunctionality(String endpoint, String testQuetsion, String parameters) throws Exception {
		// THIS COMPONENT IS DEPRECATED
		// because the intended functionality cannot be reconstructed
		// only the availability of the API endpoint can be tested
		ArrayList<Link> links = babelfyServiceFetcher().getLinksForQuestion(
				endpoint, testQuetsion, parameters
				);
	}
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
