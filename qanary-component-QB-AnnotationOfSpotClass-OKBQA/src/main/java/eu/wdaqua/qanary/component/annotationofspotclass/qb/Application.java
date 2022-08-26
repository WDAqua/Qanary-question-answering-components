package eu.wdaqua.qanary.component.annotationofspotclass.qb;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
   /* 	AnnotationofSpotClass annotationofSpotClass =  new AnnotationofSpotClass();
    	QanaryMessage yQanaryMessage= new QanaryMessage();
    	annotationofSpotClass.process(yQanaryMessage);*/
    }

    /**
     * this method is needed to make the QanaryComponent in this project known
     * to the QanaryServiceController in the qanary_component-template
     *
     * @return
     */
    @Bean
    public QanaryComponent qanaryComponent() {
        return new AnnotationofSpotClass();
    }
}
