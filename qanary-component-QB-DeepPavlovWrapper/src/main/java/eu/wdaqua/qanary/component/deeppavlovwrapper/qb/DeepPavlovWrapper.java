package eu.wdaqua.qanary.component.deeppavlovwrapper.qb;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.deeppavlovwrapper.qb.messages.DeepPavlovRequest;
import eu.wdaqua.qanary.component.deeppavlovwrapper.qb.messages.DeepPavlovResult;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import net.minidev.json.JSONArray;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class DeepPavlovWrapper extends QanaryComponent {

    private final URI endpoint;
    private final RestTemplate myRestTemplate;
    private final String langDefault;
    private final List<String> supportedLang;
    private final String applicationName;
    private final CacheOfRestTemplateResponse myCacheOfResponses;

	private static final String FILENAME_FETCH_REQUIRED_ANNOTATIONS = "/queries/fetchRequiredAnnotations.rq";
	private static final String FILENAME_STORE_COMPUTED_ANNOTATIONS = "/queries/storeComputedAnnotations.rq";
	
	private static final Logger logger = LoggerFactory.getLogger(DeepPavlovWrapper.class);

	public DeepPavlovWrapper(
			@Value("${spring.application.name}") final String applicationName, //
      @Qualifier("deeppavlov.langDefault") String langDefault, //
      @Qualifier("deeppavlov.endpoint.language.supported") List<String> supportedLang, //
      @Qualifier("deeppavlov.endpointUrl") URI endpoint, //
      RestTemplate restTemplate, //
      CacheOfRestTemplateResponse myCacheOfResponses //
			) {

        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_FETCH_REQUIRED_ANNOTATIONS);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_STORE_COMPUTED_ANNOTATIONS);

        logger.info("supportedLang: {}", supportedLang);

        assert !(endpoint == null) : //
                "endpointUrl cannot be null: " + endpoint;
        assert !(langDefault == null || langDefault.trim().isEmpty()) : //
                "langDefault cannot be null or empty: " + langDefault;
        assert (langDefault.length() == 2) : //
                "langDefault is invalid (requires exactly 2 characters, e.g., 'en'), was " + langDefault + " (length=" + langDefault.length() + ")";
        assert !(supportedLang == null || supportedLang.isEmpty()) : //
                "supportedLang cannot be null or empty: " + supportedLang;
        for (int i = 0; i < supportedLang.size(); i++) {
            assert (supportedLang.get(i).length() == 2) : //
                    "supportedLang is invalid (requires exactly 2 characters, e.g., 'en'), was " + supportedLang.get(i) + " (length=" + supportedLang.get(i).length() + ")";
        }

        this.endpoint = endpoint;
        this.langDefault = langDefault;
        this.supportedLang = supportedLang;
        this.myRestTemplate = restTemplate;
        this.applicationName = applicationName;
        this.myCacheOfResponses = myCacheOfResponses;
	}

  public URI getEndpoint() {
    return endpoint;
  }

  public String getLangDefault() {
    return langDefault;
  }

  public List<String> getSupportedLang() {
    return supportedLang;
  }

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component, some helping notes w.r.t. the typical 3 steps of implementing a
	 * Qanary component are included in the method (you might remove all of them)
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.info("process: {}", myQanaryMessage);

		// --------------------------------------------------------------------
		// STEP 1: fetch required data from the triplestore
		// --------------------------------------------------------------------

		// typical helpers 		
		QanaryUtils myQanaryUtils = this.getUtils();
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion();

    String lang = null;
        // TODO: I am not sure what the desired functionality for the actual component would be
        // but for my thesis, looking for Question.getLanguage() does *not* provide the desired
        // result:
        //  it returns the *original* language,
        //  but I want to only work with the *translation* language
        // how would this information be passed in a normal usecase?
        //
        // For my thesis it's enough to rely on setting 'langDefault' to the desired target lang
//        try {
//          lang = myQanaryQuestion.getLanguage(); // this does not work 
//          logger.info("Using language: {}", lang);
//        } catch (Exception e) {
//          lang = langDefault;
//          logger.warn("Using langDefault: {}:\n{}", lang, e.getMessage());
//        } 

        // only work with the language specified in configuration
        lang = langDefault;

    if (isLangSupported(lang) == false) {
        logger.warn("lang ({}) is not supported", lang);
        return myQanaryMessage;
    }

    String questionString = "";
    //TODO: consider: this should not be allowed to happen, because it breaks the functionality
    //TODO: BUT if there is no language annotation, the default language is used (en) 
    //-> which would then look for a translation ... 
    //so these two methods should be consolidated in some way 
    try {
      questionString = myQanaryQuestion.getTextualRepresentation(lang);
      logger.info("Using specific textual representation for language {}: {}", lang, questionString);
    } catch (Exception e) {
      logger.warn("Could not retrieve specific textual representation for language {}:\n{}", e.getMessage());
    }
    // only if no language-specific text could be found
    if (questionString.length() == 0){
        try {
            questionString = myQanaryQuestion.getTextualRepresentation();
            logger.info("Using default textual representation {}", questionString);
        } catch (Exception e) {
            logger.warn("Could not retrieve textual representation:\n{}", e.getMessage());
            // stop processing of the question, as it will not work without a question text
            return myQanaryMessage;
        }
    }

		// --------------------------------------------------------------------
		// STEP 2: compute new knowledge about the given question
		// --------------------------------------------------------------------

    try {
      DeepPavlovResult result = requestDeepPavlovWebService(endpoint, questionString, lang);
      if (result == null) {
        logger.error("No result from DeepPavlov API");
        return myQanaryMessage;
      } else {
        // --------------------------------------------------------------------
        // STEP 3: store computed knowledge about the given question into the Qanary triplestore 
        // (the global process memory)
        // --------------------------------------------------------------------
        String sparql = getSparqlInsertQuery(myQanaryQuestion, result);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
      }
    } catch (Exception e) {
      logger.error("Could not fetch result from DeepPavlov API:\n{}", e.getMessage());
    }

		return myQanaryMessage;
	}

  public DeepPavlovResult requestDeepPavlovWebService(URI uri, String question, String lang) throws RestClientException, URISyntaxException {
    DeepPavlovRequest deepPavlovRequest = new DeepPavlovRequest(uri, question, lang);
    long requestBefore = myCacheOfResponses.getNumberOfExecutedRequests();

    logger.debug("URL: {}", deepPavlovRequest.getDeepPavlovQuestionUrlAsString());
    HttpEntity<JSONArray> response = myRestTemplate.getForEntity(
        new URI(deepPavlovRequest.getDeepPavlovQuestionUrlAsString()), JSONArray.class);

    Assert.notNull(response);
    Assert.notNull(response.getBody());

    if (myCacheOfResponses.getNumberOfExecutedRequests() - requestBefore == 0) {
      logger.warn("request was cached: {}", deepPavlovRequest);
    } else {
      logger.info("request was actually executed: {}", deepPavlovRequest);
    }

    if (response.getBody().equals("[]")) {
      return null;
    } else {
      return new DeepPavlovResult(
          response.getBody(), deepPavlovRequest.getQuestion(), 
          deepPavlovRequest.getDeepPavlovEndpointUrl(), deepPavlovRequest.getLanguage());
    }

  }

  protected boolean isLangSupported(String lang) {
      for (int i = 0; i < supportedLang.size(); i++) {
          if (supportedLang.get(i).equals(lang)) {
              return true;
          }
      }
      return false;
  }

  protected String prepareResultQueryForSparqlInsert(String resultQuery) {

    // add missing prefixes
    String sparql = "PREFIX wd: <http://www.wikidata.org/entity/> "
      + "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " 
      + resultQuery;

    Query query = QueryFactory.create(sparql);
    logger.info("Query is select type: {}", query.isSelectType());
    logger.info("Query has limit: {}", query.getLimit());
    if (query.isSelectType() && query.getLimit() == Query.NOLIMIT) {
      logger.info("Query is type SELECT, but without LIMIT! Adding LIMIT 1000.");
      query.setLimit(1000);
    }

    return query.toString();
  }

    /**
     * creates the SPARQL query for inserting the data into Qanary triplestore
     * <p>
     * the data can be retrieved via SPARQL 1.1 from the Qanary triplestore using
     * QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL from qanary.commons
     * which is providing a predefined query template, s.t., the created data is
     * conform with the expectations of other Qanary components
     *
     * @param myQanaryQuestion
     * @param result
     * @return
     * @throws QanaryExceptionNoOrMultipleQuestions
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     * @throws IOException
     */
    protected String getSparqlInsertQuery(QanaryQuestion<String> myQanaryQuestion, DeepPavlovResult result) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {

      String query = prepareResultQueryForSparqlInsert(result.getSparql());

      // define here the parameters for the SPARQL INSERT query
      QuerySolutionMap bindings = new QuerySolutionMap();
      // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
      bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
      bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
      bindings.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createStringLiteral(query));
      bindings.add("confidence", ResourceFactory.createTypedLiteral(result.getConfidence()));
      bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

      // get the template of the INSERT query
      String sparql = QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL(bindings);
      logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

      return sparql;
    }
}
