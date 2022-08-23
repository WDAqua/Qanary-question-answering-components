package eu.wdaqua.qanary.component.smaph.nerd;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * this method is needed to make the QanaryComponent in this project known
	 * to the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 */
	@Bean
	public QanaryComponent qanaryComponent() {
		return new SmaphErd();
	}
}
