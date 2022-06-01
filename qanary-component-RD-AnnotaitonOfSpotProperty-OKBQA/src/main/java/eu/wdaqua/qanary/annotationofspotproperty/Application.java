package eu.wdaqua.qanary.annotationofspotproperty;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
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
		return new AnnotationofSpotProperty(applicationName);
	}
	
	
    public static void main(String[] args) throws Exception {
       SpringApplication.run(Application.class, args);
        DbpediaRecorodProperty.createDbpediaRecorodProperty();
       // QanaryMessage q = new QanaryMessage();
        //AnnotationofSpotProperty p = new AnnotationofSpotProperty();
        //p.process(q);
        
    }
}

class DbpediaRecorodProperty{
	private static TreeMap<String,String> map = new TreeMap<String,String>() ;
	//private static final Logger logger = LoggerFactory.getLogger(DbpediaRecorodProperty.class);

	public static void put(String property, String dbpediaLink) {

		map.put(property.trim(), dbpediaLink.trim());
	}

	public static TreeMap<String,String> get(){
		return map;
	}
    
	public static void print() {
		int count = 1;
		for(String s : map.keySet()) {
		System.out.println(count++ +" "+ map.get(s) + " : " + s.toString());
		}
		
	}
	public static void createDbpediaRecorodProperty() {
		
		System.out.println("Starting createDbpediaRecorodProperty()");
		try {
			
			File filename = new File("qa.qanary_component-AnnotationofSpotProperty-tgm/src/main/resources/dbpedia_3Eng_property.ttl");
			System.out.println("GetAbsolutePath(): "+filename.getAbsolutePath());

			PipedRDFIterator<org.apache.jena.graph.Triple> iter = new PipedRDFIterator<>();
			final PipedRDFStream<org.apache.jena.graph.Triple> inputStream = new PipedTriplesStream(iter);
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Runnable parser;
			parser = new Runnable() {
				@Override
				public void run() {
					RDFDataMgr.parse(inputStream, filename.getAbsolutePath());

				}
			};

			executor.submit(parser);
            executor.shutdown();
			while (iter.hasNext()) {
				org.apache.jena.graph.Triple next = iter.next();
				DbpediaRecorodProperty.put(next.getObject().toString().replaceAll("\"", "").toLowerCase(),
						next.getSubject().toString());
			//	System.out.println(iter.next().toString());
			}
			DbpediaRecorodProperty.print();
		//	executor.shutdown();
		}catch (Exception e) {
			System.out.println("Except: {}"+e);
			// TODO Auto-generated catch block
		}
		
	}
	
}