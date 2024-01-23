package eu.wdaqua.qanary.component;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

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
    
    @Bean
    public OpenAPI customOpenAPI() {
        String appVersion = getClass().getPackage().getImplementationVersion();
        return new OpenAPI().info(new Info() //
            .title("NED DBpediaSpotlight component") //
            .version(appVersion) //
            .description("This is a sample Foobar server created using springdocs - "
            + "a library for OpenAPI 3 with spring boot.")
            .termsOfService("http://swagger.io/terms/") //
            .license(new License().name("Apache 2.0").url("http://springdoc.org")) //
            );
    }
}
