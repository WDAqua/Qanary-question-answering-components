package eu.wdaqua.qanary.component;


import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    public QanaryComponent qanaryComponent(
            @Value("${spring.application.name}") final String applicationName) {
        return new KG2KGTranslateAnnotationsOfInstance(applicationName);
    }
}
