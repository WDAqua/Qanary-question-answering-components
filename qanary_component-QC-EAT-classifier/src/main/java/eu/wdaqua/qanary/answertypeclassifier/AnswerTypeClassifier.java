package eu.wdaqua.qanary.answertypeclassifier;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">GitHub wiki howto</a>
 */
public class AnswerTypeClassifier extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(AnswerTypeClassifier.class);

	@Value("${spring.application.name}")
	public String applicationName;

	@Value("${spring.application.version}")
	public String applicationVersion;

	@Value("${spring.application.classifier.endpoint}")
	private String classifierEndpoint;

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component, some helping notes w.r.t. the typical 3 steps of implementing a
	 * Qanary component are included in the method (you might remove all of them)
	 *
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);

		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		String jsonString = String.format("{\"questions\":[\"%s\"]}", myQuestion);
		StringEntity entity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

		HttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.post() //
				.setUri(this.classifierEndpoint) //
				.setEntity(entity) //
				.setHeader(HttpHeaders.ACCEPT, "application/json") //
				.build();

		logger.info("non-cached HTTP request: {}", request.getRequestLine());
		HttpResponse response = client.execute(request);
		HttpEntity responseEntity = response.getEntity();
		String json = EntityUtils.toString(responseEntity);

		// parse the response data as JSON and get the found DBpedia resources
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		JsonArray predictions = root.get("predictions").getAsJsonArray();
		String predictedClass = predictions.get(0).toString().replace("\"", "");

		// store computed knowledge about the given question into the Qanary
		// triplestore (the global process memory)

		logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
				myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
				myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));

		// push data to the Qanary triplestore
		String sparqlUpdateQuery = String.format("" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#>\n" //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>\n" //
				+ "PREFIX dbo: <http://dbpedia.org/ontology/>\n" + "\n" //
				+ "INSERT {\n" //
				+ "	GRAPH <%s> {\n" //
				+ "		?a a qa:AnnotationOfAnswerTypeClassifier .\n" //
				+ "		?a qa:hasAnswerType dbo:%s .\n" //
				+ "		\n" //
				+ "		?a oa:annotatedBy <urn:qanary:%s> .\n" //
				+ "		?a oa:annotatedAt ?time .\n" //
				+ "	}\n" //
				+ "}\n" //
				+ "WHERE {\n" //
				+ "		BIND (IRI(str(RAND())) AS ?a) .\n" //
				+ "     BIND (now() as ?time) \n" //
				+ "}", //
				myQanaryQuestion.getOutGraph(), predictedClass, this.applicationName + ":" + this.applicationVersion);

		myQanaryUtils.updateTripleStore(sparqlUpdateQuery, myQanaryMessage.getEndpoint());

		return myQanaryMessage;
	}
}
