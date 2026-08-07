package eu.wdaqua.qanary.component.smaph.nerd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
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
	public SmaphErd qanaryComponent() {
		return new SmaphErd();
	}
}
