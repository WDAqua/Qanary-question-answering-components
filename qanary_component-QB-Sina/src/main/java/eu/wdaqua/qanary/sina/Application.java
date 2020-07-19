package eu.wdaqua.qanary.sina;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

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
	 * @throws InterruptedException 
	 * @throws IOException 
	*/
	@Bean
	public QanaryComponent qanaryComponent(@Value("${sina.jarfilelocation}" ) String sinaJarFileLocation) throws IOException, InterruptedException {
		return new SINA(sinaJarFileLocation);
	}
	
	
    public static void main(String[] args) {
       SpringApplication.run(Application.class, args);
    }
}
