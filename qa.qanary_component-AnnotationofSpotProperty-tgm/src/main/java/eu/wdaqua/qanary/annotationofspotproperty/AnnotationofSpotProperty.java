package eu.wdaqua.qanary.annotationofspotproperty;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class AnnotationofSpotProperty extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(AnnotationofSpotProperty.class);

	private final String applicationName;

	public AnnotationofSpotProperty(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}


	/**
     * runCurlPOSTWithParam is a function to fetch the response from a CURL command using POST.
     */
    public static String runCurlPOSTWithParam(String weburl, String data, String contentType) throws Exception
	{
		
    	
    	//The String xmlResp is to store the output of the Template generator web service accessed via CURL command
    	
        String xmlResp = "";

    	URL url = new URL(weburl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		
		connection.setRequestProperty("Content-Type", contentType);
				
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(data);
		wr.flush();
		wr.close();
	
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		xmlResp = response.toString();
		
		logger.info("Curl Response: {}", xmlResp);
        logger.info("Response {}", xmlResp);

        return (xmlResp);

	}
	
	
	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception 
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		HashSet<String> dbLinkListSet = new HashSet<String>();
		logger.info("process: {}", myQanaryMessage);
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
        String language1 = "en";
        logger.info("Langauge of the Question: {}",language1);
        
       
       // String question =  "In which monarch did Li Si succeed someone";
        String url = "";
		String data = "";
		String contentType = "application/json";
		 
		url = "http://ws.okbqa.org:1515/templategeneration/rocknrole";
		//url  = "http://121.254.173.90:1515/templategeneration/rocknrole";
		data = "{  \"string\":\""+myQuestion+"\",\"language\":\""+language1+"\"}";
		logger.debug("data: {}", data);
		logger.debug("Component: 21");
		String output1="";
		
		// pass the input in CURL command and call the function.
		output1= AnnotationofSpotProperty.runCurlPOSTWithParam(url, data, contentType);

		logger.info("The output template is: {}",output1);

		PropertyRetrival propertyRetrival =  new PropertyRetrival();
        Property property= propertyRetrival.retrival(output1);
        
		List<MySelection> posLstl= new ArrayList<MySelection>();

				// for class
				for(String wrd:property.property){
					MySelection ms = new MySelection();
					ms.type= "AnnotationOfClass";
					ms.rsc="SpecificClass";
					ms.word= wrd;
					ms.begin=myQuestion.indexOf(wrd);
					ms.end=ms.begin+wrd.length();
					posLstl.add(ms);
		            System.out.println("Property: "+wrd);
				
				logger.info("Apply vocabulary alignment on outgraph");
	          
				String dbpediaProperty = null;

				String myKey1 = wrd.trim();
				if(myKey1!=null && !myKey1.equals("")) {
					logger.debug("searchDbLinkInTTL: {}", myKey1);
					for (Entry<String, String> e : DbpediaRecorodProperty.get().tailMap(myKey1).entrySet()) {
						    if(e.getKey().contains(myKey1)) 
						    {
						    	dbpediaProperty = e.getValue();
						      	break;
						    }
							ArrayList<String> strArrayList = new ArrayList<String>(Arrays.asList(e.getKey().split("\\s+")));

							for (String s : strArrayList)
							{
							    if(myKey1.compareTo(s) == 0) {
							    	dbpediaProperty = e.getValue();
							    }
							}
							 
							if(dbpediaProperty!=null) {
								break;
							}
					}
		         
				}

				if(dbpediaProperty!=null) {
					dbLinkListSet.add(dbpediaProperty);
				}
				logger.debug("searchDbLinkInTTL: {}", dbpediaProperty);
			}
		logger.debug("DbLinkListSet: {}", dbLinkListSet.toString());
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));

		// insert data in QanaryMessage.outgraph		
		logger.info("apply vocabulary alignment on outgraph");
		for (String urls : dbLinkListSet) {
			 String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> " //
	                 + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
	                 + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
	                 + "PREFIX dbp: <http://dbpedia.org/property/> " //
	                 + "INSERT { " //
	                 + "GRAPH <" +  myQanaryQuestion.getOutGraph()  + "> { " //
	                 + "  ?a a qa:AnnotationOfClass . " //
	                 + "  ?a oa:hasTarget [ " //
	                 + "           a    oa:SpecificResource; " //
	                 + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
	                 + "  ] ; " //
	                 + "     oa:hasBody <" + urls + "> ;" //
	                 + "     oa:annotatedBy <urn:qanary:"+this.applicationName+"> ; " //
	                 + "	    oa:annotatedAt ?time  " //
	                 + "}} " //
	                 + "WHERE { " //
	                 + "BIND (IRI(str(RAND())) AS ?a) ." //
	                 + "BIND (now() as ?time) " //
	                 + "}"; //
	         logger.info("SPARQL query: {}", sparql);
	         myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString()); 
	    }
		return myQanaryMessage;
	}

	private void loadTripleStore(String sparqlQuery, String endpoint) {
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
    }

    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }
    private class Selection {
        public int begin;
        public int end;
    }

}
