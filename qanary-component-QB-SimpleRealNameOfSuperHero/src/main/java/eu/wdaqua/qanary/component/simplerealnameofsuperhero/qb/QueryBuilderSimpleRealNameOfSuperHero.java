package eu.wdaqua.qanary.component.simplerealnameofsuperhero.qb;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url).
 * This component is a trivial demo component building a DBpedia query for
 * retrieving the real name of a superhero character (DBpedia resource)
 * identified by a previous component
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">GitHub wiki howto</a>
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary-question-answering-components/qanary_component-QB-SimpleRealNameOfSuperHero"
 *      target="_top">Intention and usage of this component</a>
 */
public class QueryBuilderSimpleRealNameOfSuperHero extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QueryBuilderSimpleRealNameOfSuperHero.class);

	private final String applicationName;

	private String supportedQuestionPrefix = "What is the real name of ";

	private String FILENAME_GET_ANNOTATION_OF_NAMED_ENTITIES = "/queries/select_all_AnnotationOfInstance.rq";
	private String FILENAME_DBPEDIA_QUERY = "/queries/dbpedia_query.rq";
	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	public QueryBuilderSimpleRealNameOfSuperHero(@Value("${spring.application.name}") String applicationName) {
		this.applicationName = applicationName;
		logger.info("application name: {}", this.getApplicationName());

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_ANNOTATION_OF_NAMED_ENTITIES);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_DBPEDIA_QUERY);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 *
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// STEP 1: get the required data
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		QanaryTripleStoreConnector triplestoreConnector = myQanaryUtils.getQanaryTripleStoreConnector();

		// STEP 2: compute new knowledge about the given question
		// in this simple case we check if the question starts with the phrase that is
		// supported by this simple query builder
		// if the question does not start with the support phrase, then nothing needs to
		// be done.
		if (!isQuestionSupported(myQuestion)) {
			logger.info("nothing to do here as question \"{}\" is not starting with \"{}\".", //
					myQuestion, supportedQuestionPrefix);
			return myQanaryMessage;
		}

		// the SPARQL query to get the annotations of named entities created by another
		// component

		QuerySolutionMap bindingsForGetAnnotationOfNamedEntities = new QuerySolutionMap();
		bindingsForGetAnnotationOfNamedEntities.add("graph",
				ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		bindingsForGetAnnotationOfNamedEntities.add("hasSource",
				ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
		bindingsForGetAnnotationOfNamedEntities.add("start", ResourceFactory.createTypedLiteral(
				String.valueOf(supportedQuestionPrefix.length()), XSDDatatype.XSDnonNegativeInteger));

		// get the template of the INSERT query
		String sparqlGetAnnotation = this.loadQueryFromFile(FILENAME_GET_ANNOTATION_OF_NAMED_ENTITIES,
				bindingsForGetAnnotationOfNamedEntities);
		logger.info("sparqlGetAnnotation: {}", sparqlGetAnnotation);
		ResultSet resultset = triplestoreConnector.select(sparqlGetAnnotation);

		while (resultset.hasNext()) {
			logger.info("Next resultset processing: {}", resultset);
			QuerySolution tupel = resultset.next();
			int start = supportedQuestionPrefix.length();
			int end = tupel.get("end").asLiteral().getInt();
			String dbpediaResource = tupel.get("hasBody").toString();
			logger.warn("found matching resource <{}> at ({},{})", dbpediaResource, start, end);

			// create the DBpedia SPARQL select query to compute the answer
			String createdDBpediaQuery = getDBpediaQuery(dbpediaResource);

			// store the created SPARQL select query (which should compute the answer) into
			// the Qanary triplestore
			String insertDataIntoQanaryTriplestoreQuery = getInsertQuery(myQanaryMessage, myQanaryQuestion,
					createdDBpediaQuery);

			// STEP 3: Store new information in the Qanary triplestore
			logger.info("The answer might be computed via: \n{}", createdDBpediaQuery);
			triplestoreConnector.update(insertDataIntoQanaryTriplestoreQuery);
		}

		return myQanaryMessage;
	}

	public boolean isQuestionSupported(String myQuestion) {
		return myQuestion.startsWith(supportedQuestionPrefix);
	}

	/**
	 * returns the SPARQL SELECT query that could retrieve the correct answer (real
	 * name of a superhero in DBpedia)
	 * 
	 * @param dbpediaResource
	 * @return
	 */
	public String getDBpediaQuery(String dbpediaResource) throws IOException {

		QuerySolutionMap bindingsForDbpediaQuery = new QuerySolutionMap();
		bindingsForDbpediaQuery.add("dbpediaResource", ResourceFactory.createResource(dbpediaResource));

		// get the template of the INSERT query
		String sparql = this.loadQueryFromFile(FILENAME_DBPEDIA_QUERY, bindingsForDbpediaQuery);
		logger.debug("DBpedia query: {}", sparql);

		return sparql;
	}

	/**
	 * returns a SPARQL INSERT query creating a new annotation that is adding the
	 * SPARQL query to the Qanary triplestore
	 * 
	 * @param myQanaryMessage
	 * @param myQanaryQuestion
	 * @param createdDBpediaQuery
	 * @return
	 * @throws SparqlQueryFailed
	 * @throws URISyntaxException
	 * @throws QanaryExceptionNoOrMultipleQuestions
	 */
	public String getInsertQuery(QanaryMessage myQanaryMessage, QanaryQuestion<String> myQanaryQuestion,
			String createdDBpediaQuery)
			throws SparqlQueryFailed, URISyntaxException, QanaryExceptionNoOrMultipleQuestions, IOException {

		QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
		bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
		bindingsForInsert.add("targetQuestion",
				ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
		bindingsForInsert.add("body", ResourceFactory.createTypedLiteral(createdDBpediaQuery, XSDDatatype.XSDstring));
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
