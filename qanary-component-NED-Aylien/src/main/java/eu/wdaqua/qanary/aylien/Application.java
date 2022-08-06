package eu.wdaqua.qanary.aylien;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.aylien.AylienServiceFetcher.Link;
import eu.wdaqua.qanary.component.QanaryComponent;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") String applicationName) throws Exception {
		return new AylienNED(applicationName);
	}

	@Bean AylienConfiguration aylienConfiguration(
		@Value("${service.endpoint}") String endpoint,
		@Value("${service.test-question}") String testQuestion
	) throws Exception {
		//TODO deprepaction?
	
		// check functionality with live API
		for (int i = 0; i < 10; i++) {
			try {
				this.testFunctionality(endpoint, testQuestion);
				logger.info("Functionality works as expected");
				break;
			} catch (Exception e) {
				logger.warn("Functionality does not work as expected on attempt no. {}:{}", i, e.toString());
			}
			if (i > 8) {
				logger.error("Functionality does not work after maximum tries. Exiting...");
				throw new Exception("Could not start component");
			}
		}
		return new AylienConfiguration(endpoint, testQuestion);
	}

	@Bean AylienServiceFetcher aylienServiceFetcher() {
		return new AylienServiceFetcher();
	}


	private void testFunctionality(String endpoint, String testQuestion) throws Exception {
		
		logger.info("TESTING: {} : \"{}\"", endpoint, testQuestion);
		ArrayList<Link> links = aylienServiceFetcher().getLinksForQuestion(
				endpoint, testQuestion);
	}






	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
