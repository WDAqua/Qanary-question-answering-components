package eu.wdaqua.qanary.component.stanford.ner;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by didier on 27.03.16.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * this method is needed to make the QanaryComponent in this project known to the
     * QanaryServiceController in the qanary_component-template
     */
    @Bean
    public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName) {
        return new StanfordNERComponent(applicationName);
    }
}
