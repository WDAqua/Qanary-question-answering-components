package eu.wdaqua.component.qb.birthdata.wikidata;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import io.swagger.v3.oas.annotations.Operation;

/**
 * represents a query builder to answer questions regarding birthplace and date
 * using Wikidata
 * <p>
 * requirements: expects a textual question to be stored in the Qanary
 * triplestore, written in English language, as well as previously annotated
 * named entities
 * <p>
 * outcome: if the question structure is supported and a previous component
 * (NED/NER) has found named entities then this component constructs a Wikidata
 * query that might be used to compute the answer to the question
 */

@Component
public class BirthDataQueryBuilder extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BirthDataQueryBuilder.class);

	public static final String FILENAME_ANNOTATIONS = "/queries/getAnnotation.rq";
	public static final String FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA = "/queries/getAnnotationOfNamedEntityLinkedToSpecificKnowledgeGraph.rq";

	public static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON = "/queries/getQuestionAnswerFromWikidataByPerson.rq";
	public static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME = "/queries/getQuestionAnswerFromWikidataByFirstnameLastname.rq";

	private static final String FIRSTNAME_ANNOTATION = "FIRST_NAME";
	private static final String LASTNAME_ANNOTATION = "LAST_NAME";

	private static final String GRAPH = "graph";
	private static final String VALUE = "value";

	private final String applicationName;

	private QanaryUtils myQanaryUtils;
	private QanaryQuestion<String> myQanaryQuestion;
	private String myQuestion;

	private final String[] supportedQuestionPatterns = { "([Ww]here and when was )(.*)( born)",
			"([Ww]here was )(.*)( born)", "([Ww]hen was )(.*)( born)" };

	private int patternIndex;

	public BirthDataQueryBuilder(@Value("$P{spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_ANNOTATIONS);
		QanaryTripleStoreConnector
				.guardNonEmptyFileFromResources(FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME);
	}

	/**
	 * compare the question against regular expression(s) representing the supported
	 * format and if a match is found, store the matched pattern index
	 *
	 * @param questionString the textual question
	 */
	@Operation(summary = "Check if the question is supported and store the matched pattern index", operationId = "isQuestionSupported", description = "Compare the question against regular expression(s) representing the supported format and if a match is found, store the matched pattern index")
	private boolean isQuestionSupported(String questionString) {
		for (int i = 0; i < this.supportedQuestionPatterns.length; i++) {
			String pattern = this.supportedQuestionPatterns[i];

			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(questionString);
			logger.info("checking pattern \"{}\"", pattern);
			if (m.find()) {
				this.patternIndex = i;
				return true;
			}
		}

		return false;
	}

	/**
	 * Find the position of a name in the textual question.
	 *
	 * @param questionString the textual question
	 * @param pattern        a regular expression (from supportedQuestionPatterns)
	 */
	@Operation(summary = "Find the index of the entity in the question", operationId = "getNamePosition", description = "Find the position of a name in the textual question." //
			+ "The name is represented as a matched group within supportedQuestionPatterns.")
	private int getNamePosition(String questionString, String pattern) {
		Matcher m = Pattern.compile(pattern).matcher(questionString);
		m.find();
		int index = m.start(2);
		return index;
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 *
	 * @param myQanaryMessage
	 * @throws Exception
	 */
	@Operation(summary = "Process a Qanary question with BirthDataQueryBuilder", //
			operationId = "process", //
			description = "Encapsulates the main functionality of this component. " //
					+ "Construct a Wikidata query to find birth date and place for named entities." //
					+ "The process can use the provided firstname and lastname or a named entity annotation.")
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// This example component requires the textual representation of the Question
		// as well as annotations of Wikidata entities made by the OpenTapioca NED.

		this.myQanaryUtils = this.getUtils(myQanaryMessage);
		this.myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
		this.myQuestion = myQanaryQuestion.getTextualRepresentation(); // get the question as String

		// STEP 1-3 have two options

		// first, try to use a named entity annotation because it is more precise if it
		// works, then stop
		myQanaryMessage = this.processForExistingNamedEntity(myQanaryMessage);
		if (myQanaryMessage != null) {
			logger.info("Found a named entity annotation. Processing finished.");
			return myQanaryMessage;
		}

//        // second, let's try to find a firstname and lastname, if that works we stop 
//        myQanaryMessage = this.processForFirstNameAndLastName(myQanaryMessage);
//        if( myQanaryMessage != null ) {
//        	logger.info("Found firstname and lastname. Processing finished.");
//        	return myQanaryMessage;
//        }

		logger.warn("Nothing could be done here.");
		return myQanaryMessage;
	}

	/**
	 * This process is only supposed to answer a specific type of question.
	 * Therefore, we only need to continue if the question asks for birthplace and
	 * date or if there is an annotation of the first and lastname.
	 * 
	 * @param myQanaryMessage
	 * @return
	 * @throws Exception
	 */
	private QanaryMessage processForFirstNameAndLastName(QanaryMessage myQanaryMessage) throws Exception {

		// STEP 1: Get the required Data
		// Get the firstname annotation if it's annotated
		QuerySolutionMap bindingsForFirstname = new QuerySolutionMap();
		bindingsForFirstname.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		bindingsForFirstname.add(VALUE, ResourceFactory.createStringLiteral(FIRSTNAME_ANNOTATION));

		String sparqlCheckFirstname = this.loadQueryFromFile(FILENAME_ANNOTATIONS, bindingsForFirstname);
		ResultSet resultsetFirstname = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlCheckFirstname);

		// Get the lastname annotation if it's annotated
		QuerySolutionMap bindingsForLastname = new QuerySolutionMap();
		// the currently used graph
		bindingsForLastname.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		// annotated for the current question
		bindingsForLastname.add(VALUE, ResourceFactory.createStringLiteral(LASTNAME_ANNOTATION));

		String sparqlCheckLastname = this.loadQueryFromFile(FILENAME_ANNOTATIONS, bindingsForLastname);
		ResultSet resultsetLastname = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlCheckLastname);

		// STEP 2: Create queries for Wikidata if the question is supported or
		// annotations are available
		ArrayList<String> queriesForAnnotation = new ArrayList<>();

		if (resultsetFirstname.hasNext() && resultsetLastname.hasNext()) {
			// In this example, we are only interested in Entities that were found from
			// another component and
			// annotated with the annotation "FIRST_NAME" and "LAST_NAME".
			queriesForAnnotation = createQueriesForAnnotation(resultsetFirstname, resultsetLastname);
		} else {
			logger.info("no annotation for {} and {} found", FIRSTNAME_ANNOTATION, LASTNAME_ANNOTATION);
		}

		if ((queriesForAnnotation.isEmpty() || queriesForAnnotation.get(0).isBlank())
				&& this.isQuestionSupported(myQuestion)) {
			// In this example we are only interested in Entities that were found at a
			// specific point
			// in the question: e.g., 'when and where was <name> born?'.
			// Because we do not require entities that might have been found anywhere else
			// in the
			// question we can filter our results:

			int filterStart = this.getNamePosition(myQuestion, this.supportedQuestionPatterns[this.patternIndex]);
			// formulate a query to find existing information
			queriesForAnnotation = createQueriesForAnnotation(filterStart);
		}

		// If no query was created, we can stop here.
		if (queriesForAnnotation.isEmpty() || queriesForAnnotation.get(0).isBlank()) {
			logger.warn("nothing to do here as question \"{}\" does not have the supported format; ", myQuestion,
					resultsetFirstname);
			return null;
		} else {
			for (int i = 0; i < queriesForAnnotation.size(); i++) {
				// store the created select query as an annotation for the current question
				// define here the parameters for the SPARQL INSERT query
				QuerySolutionMap bindings = new QuerySolutionMap();
				// use here the variable names defined in method insertAnnotationOfAnswerSPARQL
				bindings.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
				bindings.add("targetQuestion",
						ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
				bindings.add("selectQueryThatShouldComputeTheAnswer",
						ResourceFactory.createStringLiteral(queriesForAnnotation.get(i)));
				bindings.add("confidence", ResourceFactory.createTypedLiteral("1.0", XSDDatatype.XSDfloat)); // as it is
																												// rule
																												// based,
																												// a
																												// high
																												// confidence
																												// is
																												// expressed
				bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

				// get the template of the INSERT query
				String insertDataIntoQanaryTriplestoreQuery = QanaryTripleStoreConnector
						.insertAnnotationOfAnswerSPARQL(bindings);
				logger.info("SPARQL insert for adding data to Qanary triplestore: {}",
						insertDataIntoQanaryTriplestoreQuery);

				// STEP 3: Push the computed result to the Qanary triplestore
				logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
						myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
						myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
				myQanaryUtils.getQanaryTripleStoreConnector().update(insertDataIntoQanaryTriplestoreQuery);
			}

			return myQanaryMessage;
		}
	}

	private QanaryMessage processForExistingNamedEntity(QanaryMessage myQanaryMessage)
			throws IOException, SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {

		logger.info("Executing processForExistingNamedEntity.");

		String inGraph = myQanaryQuestion.getInGraph().toASCIIString();
		String outGraph = myQanaryQuestion.getOutGraph().toASCIIString();
		String myQuestionURI = myQanaryQuestion.getUri().toASCIIString();
		String endpoint = myQanaryMessage.getEndpoint().toASCIIString();

		// STEP 1: Get Named Entity from the Qanary triplestore
		int filterStart = 0;
		if (this.isQuestionSupported(myQuestion)) {
			// In this example we are only interested in Entities that were found at a
			// specific point
			// in the question: e.g., 'when and where was <name> born?'.
			filterStart = this.getNamePosition(myQuestion, this.supportedQuestionPatterns[this.patternIndex]);
		} else {
			// stop the processing
			logger.warn("processForExistingNamedEntity: Stop here as the question pattern was not found in '{}'.",
					this.myQuestion);
			return null;
		}

		QuerySolutionMap bindingsForAnnotationWithWikidataResource = new QuerySolutionMap();
		bindingsForAnnotationWithWikidataResource.add(GRAPH, ResourceFactory.createResource(inGraph));
		bindingsForAnnotationWithWikidataResource.add("regexForResourceFilter",
				ResourceFactory.createPlainLiteral("^http://www.wikidata.org/entity/"));
		bindingsForAnnotationWithWikidataResource.add("filterStart",
				ResourceFactory.createTypedLiteral(String.valueOf(filterStart), XSDDatatype.XSDint));
		String sparqlNamedEntityAnnotation = this.loadQueryFromFile(
				FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA, bindingsForAnnotationWithWikidataResource);

		// find the resources that are annotated in the given question as there are
		// possibly multiple resource, we store them in a map with the score
		ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlNamedEntityAnnotation);
		Map<String, Float> wikidataResources = new HashMap<>();
		while (resultset.hasNext()) {
			QuerySolution tupel = resultset.next();
			String wikidataResource = tupel.get("wikidataResource").asResource().getURI();
			float score = tupel.get("annotationScore").asLiteral().getFloat();

			// if the resource exists, then check if the score is higher OR no such key
			// exists
			if ((wikidataResources.containsKey(wikidataResource) && wikidataResources.get(wikidataResource) < score)
					|| (!wikidataResources.containsKey(wikidataResource))) {
				wikidataResources.put(wikidataResource, score);
			}

		}
		logger.info("found entities: {}", wikidataResources);
		if (wikidataResources.size() == 0) {
			// stop the processing
			logger.warn("processForExistingNamedEntity: Stop here as no Wikidata resources were found in the graph {}.",
					inGraph);
			return null;
		}

		// STEP 2: compute SPARQL queries that can be used to retrieve the actual answer
		ArrayList<String> queriesCapableOfRetrievingTheAnswer = new ArrayList<>(); // queries for inserting annotation
																					// of AnswerSparql into the Qanary
																					// triplestore
		for (String namedEntityResource : wikidataResources.keySet()) {
			float score = wikidataResources.get(namedEntityResource);

			String answerRepresentedAsSparqlQuery = createWikidataSparqlQuery(namedEntityResource);

			QuerySolutionMap bindingsForInserting = new QuerySolutionMap();
			bindingsForInserting.add(GRAPH, ResourceFactory.createResource(outGraph));
			bindingsForInserting.add("targetQuestion", ResourceFactory.createResource(myQuestionURI));
			bindingsForInserting.add("selectQueryThatShouldComputeTheAnswer",
					ResourceFactory.createStringLiteral(answerRepresentedAsSparqlQuery));
			// we take over the score of the named entity recognizer (NER+NED)
			bindingsForInserting.add("confidence",
					ResourceFactory.createTypedLiteral(String.valueOf(score), XSDDatatype.XSDfloat));
			bindingsForInserting.add("application",
					ResourceFactory.createResource("urn:qanary:" + this.applicationName));

			// get the template of the INSERT query to insert the new annotation into the
			// Qanary triplestore
			String insertDataIntoQanaryTriplestoreQuery = QanaryTripleStoreConnector
					.insertAnnotationOfAnswerSPARQL(bindingsForInserting);
			logger.info("created SPARQL INSERT query for adding data to Qanary triplestore: {}",
					insertDataIntoQanaryTriplestoreQuery);
			queriesCapableOfRetrievingTheAnswer.add(insertDataIntoQanaryTriplestoreQuery);
		}
		if (queriesCapableOfRetrievingTheAnswer.size() == 0) {
			// stop the processing
			logger.warn("processForExistingNamedEntity: Stop here as no queries were created (based on graph {}).",
					inGraph);
			return null;
		} else {
			logger.info(
					"Created {} SPARQL queries that should be capable of retrieving the correct answer over Wikidata.",
					queriesCapableOfRetrievingTheAnswer.size());
		}

		// STEP 3: store the created information in the Qanary triplestore as
		// AnnotationfAnswerSPARQL
		for (String query : queriesCapableOfRetrievingTheAnswer) {
			logger.info("store data in graph {} of Qanary triplestore endpoint {}", outGraph, endpoint);
			myQanaryUtils.getQanaryTripleStoreConnector().update(query);
		}

		return myQanaryMessage; // everything done
	}

	private ArrayList<String> createQueriesForAnnotation(int filterStart)
			throws IOException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
		QuerySolutionMap bindingsForAnnotation = new QuerySolutionMap();
		// the currently used graph
		bindingsForAnnotation.add(GRAPH,
				ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		// annotated for the current question
		bindingsForAnnotation.add("source", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
		// only for relevant annotations filter by starting point
		bindingsForAnnotation.add("filterStart",
				ResourceFactory.createTypedLiteral(String.valueOf(filterStart), XSDDatatype.XSDint));
		// filter resources to get only the ones that are pointing to the Wikidata
		// knowledge graph
		bindingsForAnnotation.add("regexForResourceFilter",
				ResourceFactory.createPlainLiteral("^http://www.wikidata.org/entity/"));

		String sparqlGetAnnotation = this.loadQueryFromFile(FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA,
				bindingsForAnnotation);

		// STEP 3: Compute SPARQL select queries that should produce the result for
		// every identified entity

		// Rather than computing a (textual) result this component provides a SPARQL
		// query that might be used to answer the question. This query can the used by
		// other components. This query will be stored in the Qanary triplestore.
		ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlGetAnnotation);
		ArrayList<String> queries = new ArrayList<>();
		while (resultset.hasNext()) {
			QuerySolution tupel = resultset.next();
			RDFNode wikidataResource = tupel.get("wikidataResource");
			logger.info("creating query for resource: {}", wikidataResource);
			String createdWikiDataQuery = createWikidataSparqlQuery(wikidataResource);
			queries.add(createdWikiDataQuery);
		}

		return queries;
	}

	private ArrayList<String> createQueriesForAnnotation(ResultSet resultsetFirstname, ResultSet resultsetLastname)
			throws IOException {
		ArrayList<Integer[]> firstnameStartsEnds = new ArrayList<>();
		ArrayList<Integer[]> lastnameStartsEnds = new ArrayList<>();

		while (resultsetFirstname.hasNext()) {
			Integer[] startEnd = new Integer[2];
			QuerySolution tupel = resultsetFirstname.next();
			startEnd[0] = tupel.getLiteral("start").getInt();
			startEnd[1] = tupel.getLiteral("end").getInt();

			firstnameStartsEnds.add(startEnd);
		}

		while (resultsetLastname.hasNext()) {
			Integer[] startEnd = new Integer[2];
			QuerySolution tupel = resultsetLastname.next();
			startEnd[0] = tupel.getLiteral("start").getInt();
			startEnd[1] = tupel.getLiteral("end").getInt();

			lastnameStartsEnds.add(startEnd);
		}

		ArrayList<String> queries = new ArrayList<>();
		for (int i = 0; i < firstnameStartsEnds.size(); i++) {
			String firstanme = "";
			String lastname = "";

			try {
				firstanme = myQuestion.substring(firstnameStartsEnds.get(i)[0], firstnameStartsEnds.get(i)[1]);
				lastname = myQuestion.substring(lastnameStartsEnds.get(i)[0], lastnameStartsEnds.get(i)[1]);
			} catch (Exception e) {
				logger.error("error while get first or lastname: {}", e.getMessage());
				break;
			}

			logger.info("creating query for {} {}", firstanme, lastname);

			String createdWikiDataQuery = createWikidataSparqlQuery(firstanme, lastname);
			queries.add(createdWikiDataQuery);
		}

		return queries;
	}

	public String createWikidataSparqlQuery(String wikidataResource) throws IOException {
		return this.createWikidataSparqlQuery(ResourceFactory.createResource(wikidataResource));
	}

	public String createWikidataSparqlQuery(RDFNode wikidataResource) throws IOException {
		// populate a generalized answer query with the specific entity (Wikidata ID)
		QuerySolutionMap bindingsForWikidataResultQuery = new QuerySolutionMap();
		// set expected person as parameter for Wikidata query
		bindingsForWikidataResultQuery.add("person", wikidataResource);
		return this.loadQueryFromFile(FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON, bindingsForWikidataResultQuery);
	}

	public String createWikidataSparqlQuery(String firstname, String lastname) throws IOException {
		// populate a generalized answer query with the specific entity (Wikidata ID)
		QuerySolutionMap bindingsForWikidataResultQuery = new QuerySolutionMap();
		// set expected last and firstname as parameter for Wikidata query
		bindingsForWikidataResultQuery.add("firstnameValue", ResourceFactory.createLangLiteral(firstname, "en"));
		bindingsForWikidataResultQuery.add("lastnameValue", ResourceFactory.createLangLiteral(lastname, "en"));
		return this.loadQueryFromFile(FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME,
				bindingsForWikidataResultQuery);
	}
}
