package eu.wdaqua.qanary.component.ned.gpt;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.ned.gpt.openai.OpenAiApiFetchingServiceFailedException;
import eu.wdaqua.qanary.component.ned.gpt.openai.OpenAiApiService;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

@Component
/**
 * This component uses the Open AI API to identify Wikipedia resources in the
 * given text, links it to the corresponding DBpedia resources, and stores it as
 * annotations of type AnnotationOfInstance
 * 
 * Remark: the prompt is not computing the positions as the GPT models are
 * constantly bad with providing the correct indexes inside of the given text
 */
public class GptBasedNamedEntityDisambiguation extends QanaryComponent {
	private static final String FILENAME_SELECT_MAPPINGS_FROM_DBPEDIA = "/queries/select_mappings_from_DBpedia.rq";
	private static final String FILENAME_INSERT_ONE_ANNOTATIONOFINSTANCE = "/queries/insert_one_AnnotationOfInstance.rq";

	private static final String NAMEDENTITY = "named_entity";
	private static final String WIKIPEDIAURL = "wikipedia_url";

	private static final String DBPEDIAENDPOINT = "https://dbpedia.org/sparql";

	private static Map<URI, URI> cachedNamedEntityMappings;

	private static final Logger LOGGER = LoggerFactory.getLogger(GptBasedNamedEntityDisambiguation.class);

	private final String applicationName;

	private final OpenAiApiService myOpenAiApiService;

	private String promptTemplate = """
			Assume you are a Named Entity Recognizer and Named Entity Disambiguator. Given the following sentence, provide the Wikipedia article URL that is describing the found Named Entity, its start and end index in the given sentence.
			Create a valid JSON array using the format: [{"named_entity": "string", "wikipedia_url": "url"}]
			The sentence: %s
			The JSON object is:""";

	private String model;
	private String defaultScore;

	public GptBasedNamedEntityDisambiguation( //
			@Value("${spring.application.name}") final String applicationName, //
			@Value("${openai.api.live.test.active}") boolean doApiIsAliveCheck, //
			@Value("${openai.api.live.test.question}") String liveTestQuestion, //
			@Value("${openai.gpt.model}") String model, //
			@Value("${openai.api.defaultScore}") String defaultScore, //
			OpenAiApiService myOpenAiApiService //
	) throws URISyntaxException, MissingArgumentException, OpenAiApiFetchingServiceFailedException {
		this.applicationName = applicationName;
		this.myOpenAiApiService = myOpenAiApiService;
		cachedNamedEntityMappings = new HashMap<>();
		this.model = model;
		this.defaultScore = defaultScore;

		// here if the files are available and do contain content
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_MAPPINGS_FROM_DBPEDIA);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ONE_ANNOTATIONOFINSTANCE);

		if (doApiIsAliveCheck) {
			LOGGER.warn("Live test will be executed.");
			this.getCompletion(liveTestQuestion, model);
		} else {
			LOGGER.info("No live test will be executed.");
		}
	}

	/**
	 * process the task handed from the Qanary pipeline
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		LOGGER.info("process: {}", myQanaryMessage);

		// typical helpers
		QanaryUtils myQanaryUtils = this.getUtils();

		// --------------------------------------------------------------------
		// STEP 1: get the textual representation of the question from the Qanary
		// triplestore (the global process memory)
		// --------------------------------------------------------------------
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion();
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		// --------------------------------------------------------------------
		// STEP 2: compute Named Entities from the given question and map them to Bpedia
		// --------------------------------------------------------------------
		List<NamedEntity> results = this.getNamedEntitiesFromCompletion(myQuestion, this.model);
		List<NamedEntity> resultsWithDBpediaResource = this.getDBpediaResourcesForNamedEntities(results);

		// --------------------------------------------------------------------
		// STEP 3: Store the created data to the Qanary triplestore
		// --------------------------------------------------------------------
		for (NamedEntity myNamedEntity : resultsWithDBpediaResource) {
			String sparql = createInsertQuery(myQanaryQuestion, myNamedEntity);
			LOGGER.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql); // insert into Qanary triplestore
		}

		return myQanaryMessage;
	}

	/**
	 * create DBpedia resources mappings for the given named entities
	 * 
	 * @param namedEntities
	 * @return
	 * @throws IOException
	 */
	public List<NamedEntity> getDBpediaResourcesForNamedEntities(List<NamedEntity> namedEntities) throws IOException {
		List<NamedEntity> resultsWithDBpediaResource = new LinkedList<>();

		Map<URI, URI> resourceMappingToDBpedia = updateAndGetResourceMapping(namedEntities);

		if (resourceMappingToDBpedia != null) {
			for (NamedEntity namedEntity : namedEntities) {
				URI dbpediaResource = resourceMappingToDBpedia.get(namedEntity.getResource());
				if (dbpediaResource != null) {
					resultsWithDBpediaResource.add(new NamedEntity(namedEntity, dbpediaResource));
				}
			}
		} else {
			LOGGER.error("Map resultsWithDBpediaResource is null, should not happen.");
		}

		LOGGER.info("{} results with DBpedia resources: {}", resultsWithDBpediaResource.size(),
				resultsWithDBpediaResource);
		return resultsWithDBpediaResource;
	}

	/**
	 * compute mappings of Wikipedia URLs to DBpedia URIs
	 * 
	 * @param namedEntities
	 * @return
	 * @throws IOException
	 */
	private Map<URI, URI> updateAndGetResourceMapping(List<NamedEntity> namedEntities) throws IOException {

		// get all resources
		List<URI> resourcesThatNeedToBeMapped = new LinkedList<>();
		for (NamedEntity namedEntity : namedEntities) {
			if (namedEntity.getResource() != null) {
				resourcesThatNeedToBeMapped.add(namedEntity.getResource());

				// init if required
				if (!cachedNamedEntityMappings.containsKey(namedEntity.getResource())) {
					cachedNamedEntityMappings.putIfAbsent(namedEntity.getResource(), null);
				}
			}
		}

		LOGGER.debug("resourcesThatNeedToBeMapped: size={}", resourcesThatNeedToBeMapped.size());

		int counter = 0;
		for (URI resource : resourcesThatNeedToBeMapped) {
			LOGGER.debug("{}. Compute mappings for {}", counter++, resource.toASCIIString());
			QuerySolutionMap bindings = new QuerySolutionMap();
			bindings.add("wikipedia_url", ResourceFactory.createResource(resource.toASCIIString()
					.replace("https://en.wikipedia.org/wiki/", "http://en.wikipedia.org/wiki/")));

			String sparqlQueryToGetMappings = this.loadQueryFromFile(FILENAME_SELECT_MAPPINGS_FROM_DBPEDIA, bindings);
			LOGGER.debug("Get mappings for {} using: {}", resource.toASCIIString(), sparqlQueryToGetMappings);

			@SuppressWarnings("deprecation")
			QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIAENDPOINT, sparqlQueryToGetMappings);

			try {
				ResultSet results = qexec.execSelect();

				while (results.hasNext()) {
					Binding result = results.nextBinding();
					URI dbpedia_resource = new URI(result.get("dbpedia_resource").getURI());
					cachedNamedEntityMappings.put(resource, dbpedia_resource); // store for later usage
				}
			} catch (Exception e) {
				LOGGER.warn("could not query {} endpoint {}", DBPEDIAENDPOINT, e.getMessage());
			} finally {
				qexec.close();
			}

			for (Entry<URI, URI> entry : cachedNamedEntityMappings.entrySet()) {
				LOGGER.debug("Available mappings: {} -> {}", entry.getKey(), entry.getValue());
			}
		}
		return cachedNamedEntityMappings;
	}

	/**
	 * create SPARQL INSERT query for storing a named entity annotation
	 * (AnnotationOfInstance) into the Qanary triplestore
	 * 
	 * @param myQanaryQuestion
	 * @param myNamedEntity
	 * @return
	 * @throws QanaryExceptionNoOrMultipleQuestions
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 * @throws IOException
	 */
	public String createInsertQuery( //
			QanaryQuestion<String> myQanaryQuestion, //
			NamedEntity myNamedEntity //
	) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
		QuerySolutionMap bindings = new QuerySolutionMap();
		// use here the variable names defined in method insertAnnotationOfAnswerSPARQL
		bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
		bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

		XSDDatatype intType = XSDDatatype.XSDinteger;
		XSDDatatype floatType = XSDDatatype.XSDfloat;
		bindings.add("start", ResourceFactory.createTypedLiteral(myNamedEntity.getStartAsString(), intType));
		bindings.add("end", ResourceFactory.createTypedLiteral(myNamedEntity.getEndAsString(), intType));
		bindings.add("resource", ResourceFactory.createResource(myNamedEntity.getResource().toASCIIString()));
		bindings.add("score", ResourceFactory.createTypedLiteral(String.format(this.defaultScore), floatType));

		// get the template of the INSERT query and apply bindings
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_INSERT_ONE_ANNOTATIONOFINSTANCE,
				bindings);
	}

	/**
	 * executing a request to the OpenAI API
	 * 
	 * @param prompt
	 * @return
	 * @throws OpenAiApiFetchingServiceFailedException
	 */
	public JsonArray getCompletion(String prompt, String model) throws OpenAiApiFetchingServiceFailedException {

		String fullPrompt = String.format(promptTemplate, prompt);

		LOGGER.info("fullPrompt: {}", fullPrompt);

		int numberOfExecutedRequests = OpenAiApiService.getNumberOfExecutedRequests();
		List<ChatCompletionChoice> choices = myOpenAiApiService.getCompletion(fullPrompt, model);
		if (numberOfExecutedRequests == OpenAiApiService.getNumberOfExecutedRequests()) {
			LOGGER.warn("The result was provided by the API cache.");
		}

		int counter = 0;
		for (ChatCompletionChoice chatCompletionChoice : choices) {
			LOGGER.info("{}. result: {}", counter++, chatCompletionChoice.getMessage().getContent());
		}

		// return only the first result
		return JsonParser.parseString(choices.get(0).getMessage().getContent()).getAsJsonArray();
	}

	/**
	 * for the given prompt return the GPT API's results only if they are valid
	 * 
	 * @param sentence
	 * @return
	 * @throws OpenAiApiFetchingServiceFailedException
	 */
	public List<NamedEntity> getNamedEntitiesFromCompletion(String sentence, String model)
			throws OpenAiApiFetchingServiceFailedException {
		JsonArray results = this.getCompletion(sentence, model);

		List<NamedEntity> cleanedResults = new LinkedList<>();

		for (int i = 0; i < results.size(); i++) {
			JsonObject result = results.get(i).getAsJsonObject();
			// check for availability of requested data structure
			if (!(result.get(NAMEDENTITY) != null)) {
				LOGGER.error("could not find '{}' in response.", NAMEDENTITY);
			} else if (!(result.get(WIKIPEDIAURL) != null)) {
				LOGGER.error("could not find '{}' in response.", WIKIPEDIAURL);
			} else if (!sentence.contains(result.get(NAMEDENTITY).getAsString())) {
				LOGGER.error("could not find string '{}' in given sentence '{}'.",
						result.get(NAMEDENTITY).getAsString(), sentence);
			} else {
				URI resource = null;
				// if not empty or null, then the URI needs to be parsable
				if (!result.get(WIKIPEDIAURL).getAsString().isBlank()) {
					try {
						resource = new URI(result.get(WIKIPEDIAURL).getAsString());
					} catch (Exception e) {
						LOGGER.error("could not create a URI from resource {}", result.get(WIKIPEDIAURL).getAsString());
						continue;
					}
				}

				LOGGER.info("{}. result created by GPT model is consistent.", i);
				int start = sentence.indexOf(result.get(NAMEDENTITY).getAsString());
				int end = start + result.get(NAMEDENTITY).getAsString().length();
				cleanedResults.add(new NamedEntity(start, end, result.get(NAMEDENTITY).getAsString(), resource));
			}
		}

		LOGGER.info("computed JSON array: {}", results.toString());
		LOGGER.info("cleaned JSON array: {}", cleanedResults.toString());

		return cleanedResults;
	}

	/**
	 * load prepared statement from queries folder (it will check the qanary.commons
	 * folder, too) and map the given variable bindings into the returned SPARQL
	 * query
	 * 
	 * @param filenameWithRelativePath
	 * @param bindings
	 * @return
	 * @throws IOException
	 */
	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}
}
