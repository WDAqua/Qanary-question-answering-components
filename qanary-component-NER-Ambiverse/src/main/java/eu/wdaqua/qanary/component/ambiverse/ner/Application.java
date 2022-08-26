package eu.wdaqua.qanary.component.ambiverse.ner;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.TreeMap;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName) {
		return new AmbiverseNER(applicationName);
	}
	
	
    public static void main(String[] args) throws Exception {
    SpringApplication.run(Application.class, args);
    }
    
    
}

class DbpediaRecorodProperty{
	private static TreeMap<String,String> map = new TreeMap<String,String>() ;

	public static void put(String property, String dbpediaLink) {

		map.put(property, dbpediaLink);
	}

	public static TreeMap<String,String> get(){
		return map;
	}

}
