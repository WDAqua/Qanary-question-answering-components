package eu.wdaqua.qanary.mypackage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@EnableAutoConfiguration
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
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName) {
		return new Babelfy(applicationName);
	}
	
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
