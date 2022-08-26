package eu.wdaqua.qanary.component.agdistis.ned;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
public class Application {


    @Bean
    public QanaryComponent qanaryComponent() {
        return new Agdistis();
    }

    /**
     * default main, can be removed later
     */
    public static void main(String[] args) {
        //Properties p = new Properties();
        //new SpringApplicationBuilder(Application.class).properties(p).run(args);
        SpringApplication.run(Application.class, args);
    }
}
