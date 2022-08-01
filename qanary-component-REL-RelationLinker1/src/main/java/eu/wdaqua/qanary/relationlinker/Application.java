package eu.wdaqua.qanary.relationlinker;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.relationlinker.RelationLinkerServiceFetcher.Link;

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
	* @throws Exception
	*/
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName) throws Exception {
		return new RelationLinker1(applicationName);
	}

	@Bean
	public RelationLinkerConfiguration relationLinkerConfiguration(
			@Value("${service.endpoint}") final String endpoint,
			@Value("${service.test-question}") final String testQuetsion) 
	throws Exception {
		// check functionality with live API
		for (int i = 0; i < 10; i++) {
			try {
				this.testFunctionality(endpoint, testQuetsion);
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
		return new RelationLinkerConfiguration(endpoint, testQuetsion);
	}

	@Bean RelationLinkerServiceFetcher relationLinkerServiceFetcher() {
		return new RelationLinkerServiceFetcher();
	}

	// test the internal functionality of this component, using live APIs
	private void testFunctionality(String endpoint, String testQuetsion) throws Exception {
		// THIS COMPONENT IS DEPRECATED
		// because the intended functionality cannot be reconstructed
		// only the availability of the API endpoint can be tested
		ArrayList<Link> links = relationLinkerServiceFetcher().getLinksForQuestion(
				testQuetsion, endpoint
				);
	}
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
