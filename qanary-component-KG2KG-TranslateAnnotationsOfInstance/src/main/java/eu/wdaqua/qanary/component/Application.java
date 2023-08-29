package eu.wdaqua.qanary.component;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
public class Application
{
    @Bean
    public QanaryComponent qanaryComponent(
            @Value("${spring.application.name}") final String applicationName) {
        return new KG2KGTranslateAnnotationsOfInstance(applicationName);
    }

    @Autowired
    public QanaryComponentConfiguration qanaryComponentConfiguration;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
