package eu.wdaqua.qanary.component.copyvaluesofpriorgraph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.QanaryComponentConfiguration;

/**
 * Basic class for wrapping the functionality of this Qanary component.
 * Note: there is normally no need to change anything here.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
public class Application {

	/**
	 * makes the component known to the QanaryServiceController in the qa.component
	 * framework; the concrete return type also lets the component's own controller
	 * autowire it (a single bean serves both the framework and the controller).
	 */
	@Bean
	public CopyValuesOfPriorGraph qanaryComponent(
			@Value("${spring.application.name}") final String applicationName,
			@Value("${spring.boot.admin.url}") final String adminUrl,
			RestTemplate restTemplate) {
		return new CopyValuesOfPriorGraph(applicationName, adminUrl, restTemplate);
	}

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
