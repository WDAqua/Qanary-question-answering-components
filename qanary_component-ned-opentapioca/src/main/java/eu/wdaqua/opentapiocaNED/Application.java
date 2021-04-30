package eu.wdaqua.opentapiocaNED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

	@Bean
	public OpenTapiocaConfiguration openTapiocaConfiguration( //
			@Value("${opentapioca.endpoint:https://opentaiopca.org/api}") String endpoint
	) {
			return new OpenTapiocaConfiguration(endpoint);
	}

	@Bean
	public QanaryComponent qanaryComponent() {
		return new OpenTapiocaNED();
	}

	@Bean
	public OpenTapiocaServiceFetcher openTapiocaServiceFetcher() {
		return new OpenTapiocaServiceFetcher();
	}
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
