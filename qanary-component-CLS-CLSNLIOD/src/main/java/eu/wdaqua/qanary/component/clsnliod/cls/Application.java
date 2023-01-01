package eu.wdaqua.qanary.component.clsnliod.cls;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		DbpediaRecorodClass.createDbpediaRecorodClass();
	}

	/**
	 * this method is needed to make the QanaryComponent in this project known
	 * to the QanaryServiceController in the qanary_component-template
	 *
	 * @return
	 */
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName,
										   @Value("${cls-clsnliod.cache.enabled}") final Boolean cacheEnabled,
										   @Value("${cls-clsnliod.cache.file}") final String cacheFile) {
		return new ClsNliodCls(applicationName, cacheEnabled, cacheFile);
	}
}

class DbpediaRecorodClass {

	private static final Logger logger = LoggerFactory.getLogger(DbpediaRecorodClass.class);

	private static TreeMap<String, String> map = new TreeMap<String, String>();
	//private static final Logger logger = LoggerFactory.getLogger(DbpediaRecorodProperty.class);

	public static void put(String property, String dbpediaLink) {

		map.put(property.trim(), dbpediaLink.trim());
	}

	public static TreeMap<String, String> get() {
		return map;
	}

	public static void print() {
		int count = 1;
		for (String s : map.keySet()) {
			System.out.println(count++ + " " + map.get(s) + " : " + s.toString());
		}

	}

	public static void createDbpediaRecorodClass() {

		logger.info("Starting createDbpediaRecorodProperty()");
		try {
			File filename = new File("qanary_component-CLS-CLSNLIOD/src/main/resources/dbpedia_3Eng_class.ttl");
			//File filename = new File("src/main/resources/dbpedia_3Eng_class.ttl");
			logger.info(filename.getAbsolutePath());

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
				DbpediaRecorodClass.put(next.getObject().toString().replaceAll("\"", "").toLowerCase(),
						next.getSubject().toString());
				//	System.out.println(iter.next().toString());
			}
			DbpediaRecorodClass.print();
			//	executor.shutdown();
		} catch (Exception e) {
			logger.error("Except: {}" + e);
			// TODO Auto-generated catch block
		}

	}

}

class PropertyRetrival {
	private static final Logger logger = LoggerFactory.getLogger(PropertyRetrival.class);

	public Property retrival(String s) {
		Property p = new Property();
		//String input="";

		//String keyWords[] = {"Property","Resource","Literal","Class",""};
		/*List<String> property = new ArrayList<String>();
		List<String> resource = new ArrayList<String>();
		List<String> resourceL = new ArrayList<String>();
		List<String> classRdf = new ArrayList<String>();*/
		List<String> tempList = new ArrayList<String>();

		try {

			JSONArray json = new JSONArray(s);

			//JSONArray characters = (JSONArray) json.get("slots");
			Iterator i = json.iterator();
			while (i.hasNext()) {
				JSONObject mainObject = (JSONObject) i.next();
				JSONArray slots = (JSONArray) mainObject.get("slots");

				Iterator q_itr = slots.iterator();

				String prevSub = "";
				while (q_itr.hasNext()) {


					JSONObject qstn = (JSONObject) q_itr.next();

					String sub = (String) qstn.get("s");
					String obj = (String) qstn.get("o");


					if (obj.contains("rdf:")) {
						tempList.clear();
						String kWords[] = null;
						if (obj.contains("|")) {
							String t[] = obj.split("\\|");
							int cn = 0;
							kWords = new String[t.length];
							for (String tw : t) {
								kWords[cn++] = tw.substring(tw.indexOf(":") + 1);
							}
						} else {
							kWords = new String[1];
							kWords[0] = obj.substring(obj.indexOf(":") + 1);
						}
						for (String word : kWords) {
							tempList.add(word);
						}
						prevSub = sub;
					} else {
						if (prevSub.equalsIgnoreCase(sub)) {
							//System.out.println("Inside==============================");
							//	System.out.println("TempList: "+tempList.toString());
							for (String temp : tempList) {
								switch (temp) {
									case "Property":
										if (!p.property.contains(obj))
											p.property.add(obj);
										break;
									case "Resource":
										if (!p.resource.contains(obj))
											p.resource.add(obj);
										break;
									case "Literal":
										if (!p.resourceL.contains(obj))
											p.resourceL.add(obj);
										break;
									case "Class":
										if (!p.classRdf.contains(obj))
											p.classRdf.add(obj);
										break;
								}
							}
						}
						prevSub = "";
						tempList.clear();
					}

				}

			}

			//System.out.println("List of Subjects: "+ids.toString());
		} catch (Exception e) {
			logger.error("Except: {}" + e);
			e.printStackTrace();
		}
		return p;
		/*System.out.println("\nThe rdf:Resource List : "+resource.toString());
		System.out.println("\nThe rdf:Property List : "+property.toString());
		System.out.println("\nThe rdf:Literal List : "+resourceL.toString());
		System.out.println("\nThe rdf:Class List : "+classRdf.toString());*/
	}
}

class Property {
	public List<String> property = new ArrayList<String>();
	public List<String> resource = new ArrayList<String>();
	public List<String> resourceL = new ArrayList<String>();
	public List<String> classRdf = new ArrayList<String>();
}

class MySelection {
	public String word = "";
	public String type = "";
	public String rsc = "";
	public int begin;
	public int end;
}
