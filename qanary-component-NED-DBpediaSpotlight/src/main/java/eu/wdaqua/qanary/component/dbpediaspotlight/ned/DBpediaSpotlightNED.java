package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * represents a wrapper of the DBpedia Spotlight service used as NED annotator
 * <p>
 * requirements: this Qanary service expects as input a textual question (that
 * is stored in the Qanary triplestore) written using English language
 * <p>
 * outcome: if DBpedia Spotlight has recognized named entities and was enabled
 * to link them to DBpedia, then this information is added to the Qanary
 * triplestore to be used by following services of this question answering
 * process
 *
 * @author Kuldeep Singh, Dennis Diefenbach, Andreas Both
 */

@Component
public class DBpediaSpotlightNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNED.class);

	@Autowired
	CacheOfRestTemplateResponse myCacheOfResponses;

	RestTemplate restTemplate;

	private DBpediaSpotlightServiceFetcher myDBpediaSpotlightServiceFetcher;

	@Inject
	private DBpediaSpotlightConfiguration myDBpediaSpotlightConfiguration;

	private boolean ignoreSslCertificate;

	private final String applicationName;
	private static final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	public DBpediaSpotlightNED( //
			@Value("${spring.application.name}") final String applicationName, //
			@Value("${dbpediaspotlight.endpoint.ssl.certificatevalidation.ignore:false}") final boolean ignore, //
			@Autowired DBpediaSpotlightServiceFetcher myDBpediaSpotlightServiceFetcher,
			RestTemplateWithCaching restTemplate //
	) {
		this.applicationName = applicationName;
		this.restTemplate = restTemplate;
		this.myDBpediaSpotlightServiceFetcher = myDBpediaSpotlightServiceFetcher;
		this.setIgnoreSslCertificate(ignore);

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
	}

	public void setIgnoreSslCertificate(boolean flag) {
		this.ignoreSslCertificate = flag;
	}

	public boolean getIgnoreSslCertificate() {
		return this.ignoreSslCertificate;
	}

	/**
	 * deactivates the SSL certificate validation for the restTemplate
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 */
	private void deactivateRestTemplateCertificateValidation()
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {

		this.restTemplate.setRequestFactory(getRequestFactoryForSslVerficationDeactivation());
		logger.warn("SSL certifcate validation deactivated for DBpedia spotlight server");
	}

	/**
	 * create a request factory capable of bypassing SSL certificates
	 * 
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	protected static HttpComponentsClientHttpRequestFactory getRequestFactoryForSslVerficationDeactivation()
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

		SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy)
				.build();

		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

		CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

		requestFactory.setHttpClient(httpClient);

		return requestFactory;
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		// deactivate SSL check if demanded
		if (this.getIgnoreSslCertificate()) {
			deactivateRestTemplateCertificateValidation();
		}

		// STEP 1: Retrieve the information needed for the computations
		// i.e., retrieve the current question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("process question \"{}\" with DBpedia Spotlight at '{}' and minimum confidence: {}", //
				myQuestion, myDBpediaSpotlightConfiguration.getEndpoint(),
				myDBpediaSpotlightConfiguration.getConfidenceMinimum());

		// STEP2: Call the DBpedia NED service
		JsonArray resources = myDBpediaSpotlightServiceFetcher.getJsonFromService(myQuestion);

		// get all found DBpedia resources
		List<FoundDBpediaResource> foundDBpediaResources = myDBpediaSpotlightServiceFetcher
				.getListOfResources(resources);

		// STEP3: Push the result of the component to the triplestore

		// TODO: prevent that duplicate entries are created within the
		// triplestore, here the same data is added as already exit (see
		// previous SELECT query)

		// TODO: create one larger SPARQL INSERT query that adds all discovered named
		// entities at once
		for (FoundDBpediaResource found : foundDBpediaResources) {
			String sparql = getSparqlInsertQuery(found, myQanaryQuestion);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
		}

		return myQanaryMessage;
	}

	public String getSparqlInsertQuery(FoundDBpediaResource found, QanaryQuestion<String> myQanaryQuestion) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException{
			QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
			bindingsForInsert.add("graph",
					ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
			bindingsForInsert.add("targetQuestion",
					ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
			bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(found.getBegin()),
					XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(found.getEnd()),
					XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("answer", ResourceFactory.createResource(found.getResource().toString()));
			bindingsForInsert.add("score", ResourceFactory
					.createTypedLiteral(String.valueOf(found.getSimilarityScore()), XSDDatatype.XSDdecimal));
			bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

			// get the template of the INSERT query
			String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
			logger.info("SPARQL query: {}", sparql);
			return sparql;
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}

}
