package eu.wdaqua.component.qb.birthdata.wikidata;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import io.swagger.v3.oas.annotations.Operation;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * represents a query builder to answer questions regarding birthplace and date using Wikidata
 * <p>
 * requirements: expects a textual question to be stored in the Qanary triplestore,
 * written in English language, as well as previously annotated named entities
 * <p>
 * outcome: if the question structure is supported and a previous component (NED/NER) has found
 * named entities then this component constructs a Wikidata query that might be used to compute
 * the answer to the question
 */

@Component
public class BirthDataQueryBuilder extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(BirthDataQueryBuilder.class);

    private static final String FILENAME_ANNOTATIONS = "/queries/getAnnotation.rq";
    private static final String FILENAME_ANNOTATIONS_FILTERED = "/queries/getAnnotationFiltered.rq";

    private static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON = "/queries/getQuestionAnswerFromWikidataByPerson.rq";
    private static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME = "/queries/getQuestionAnswerFromWikidataByFirstnameLastname.rq";

    private static final String FIRSTNAME_ANNOTATION = "FIRST_NAME";
    private static final String LASTNAME_ANNOTATION = "LAST_NAME";

    private static final String GRAPH = "graph";
    private static final String VALUE = "value";

    private final String applicationName;

    private QanaryUtils myQanaryUtils;
    private QanaryQuestion<String> myQanaryQuestion;
    private String myQuestion;

    private final String[] supportedQuestionPatterns = {
            "([Ww]here and when was )(.*)( born)",
            "([Ww]here was )(.*)( born)",
            "([Ww]hen was )(.*)( born)"
    };

    private int patternIndex;

    public BirthDataQueryBuilder(@Value("$P{spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * compare the question against regular expression(s) representing the supported format
     * and if a match is found, store the matched pattern index
     *
     * @param questionString the textual question
     */
    @Operation(
            summary = "Check if the question is supported and store the matched pattern index",
            operationId = "isQuestionSupported",
            description = "Compare the question against regular expression(s) representing the supported format and if a match is found, store the matched pattern index"
    )
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
    @Operation(
            summary = "Find the index of the entity in the question",
            operationId = "getNamePosition",
            description = "Find the position of a name in the textual question." //
                    + "The name is represented as a matched group within supportedQuestionPatterns."
    )
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
    @Operation(
            summary = "Process a Qanary question with BirthDataQueryBuilder", //
            operationId = "process", //
            description = "Encapsulates the main functionality of this component. " //
                    + "Construct a Wikidata query to find birth date and place for named entities."
    )
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);

        // STEP 1: Get the required Data
        //
        // This example component requires the textual representation of the Question
        // as well as annotations of Wikidata entities made by the OpenTapioca NED.

        // get the question as String
        this.myQanaryUtils = this.getUtils(myQanaryMessage);
        this.myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        this.myQuestion = myQanaryQuestion.getTextualRepresentation();

        // This component is only supposed to answer a specific type of question.
        // Therefore, we only need to continue if the question asks for birthplace and date or if there is an
        // annotation of the first and lastname.


        // Get the firstname annotation if it's annotated
        QuerySolutionMap bindingsForFirstname = new QuerySolutionMap();
        bindingsForFirstname.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForFirstname.add(VALUE, ResourceFactory.createStringLiteral(FIRSTNAME_ANNOTATION));

        String sparqlCheckFirstname = this.loadQueryFromFile(FILENAME_ANNOTATIONS, bindingsForFirstname);
        ResultSet resultsetFirstname = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlCheckFirstname);

        // Get the lastname annotation, if it's annotated
        QuerySolutionMap bindingsForLastname = new QuerySolutionMap();
        // the currently used graph
        bindingsForLastname.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        // annotated for the current question
        bindingsForLastname.add(VALUE, ResourceFactory.createStringLiteral(LASTNAME_ANNOTATION));

        String sparqlCheckLastname = this.loadQueryFromFile(FILENAME_ANNOTATIONS, bindingsForLastname);
        ResultSet resultsetLastname = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlCheckLastname);


        // STEP 2: Create queries for Wikidata if the question is supported or annotations are available
        ArrayList<String> queriesForAnnotation = new ArrayList<>();

        if (resultsetFirstname.hasNext() && resultsetLastname.hasNext()) {
            // In this example, we are only interested in Entities that were found from another component and
            // annotated with the annotation "FIRST_NAME" and "LAST_NAME".
            queriesForAnnotation = createQueriesForAnnotation(resultsetFirstname, resultsetLastname);
        } else {
        	logger.info("no annotation for {} and {} found", FIRSTNAME_ANNOTATION, LASTNAME_ANNOTATION);
        }

        if ((queriesForAnnotation.isEmpty() || queriesForAnnotation.get(0).isBlank()) && this.isQuestionSupported(myQuestion)) {
            // In this example we are only interested in Entities that were found at a specific point
            // in the question: e.g., 'when and where was <name> born?'.
            // Because we do not require entities that might have been found anywhere else in the
            // question we can filter our results:

            int filterStart = this.getNamePosition(myQuestion, this.supportedQuestionPatterns[this.patternIndex]);
            // formulate a query to find existing information
            queriesForAnnotation = createQueriesForAnnotation(filterStart);

        }

        // If no query was created, we can stop here.
        if (queriesForAnnotation.isEmpty() || queriesForAnnotation.get(0).isBlank() ) {
            logger.warn("nothing to do here as question \"{}\" does not have the supported format", myQuestion);
            return myQanaryMessage;
        }


        for (int i = 0; i < queriesForAnnotation.size(); i++) {
            // store the created select query as an annotation for the current question
            // define here the parameters for the SPARQL INSERT query
            QuerySolutionMap bindings = new QuerySolutionMap();
            // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
            bindings.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindings.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createStringLiteral(queriesForAnnotation.get(i)));
            bindings.add("confidence", ResourceFactory.createTypedLiteral("1.0", XSDDatatype.XSDfloat)); // as it is rule based, a high confidence is expressed
            bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            String insertDataIntoQanaryTriplestoreQuery = QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL(bindings);
            logger.info("SPARQL insert for adding data to Qanary triplestore: {}", insertDataIntoQanaryTriplestoreQuery);

            //STEP 4: Push the computed result to the Qanary triplestore
            logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
                    myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
                    myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
            myQanaryUtils.getQanaryTripleStoreConnector().update(insertDataIntoQanaryTriplestoreQuery);
        }

        return myQanaryMessage;
    }

    private ArrayList<String> createQueriesForAnnotation(int filterStart) throws IOException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
        QuerySolutionMap bindingsForAnnotation = new QuerySolutionMap();
        // the currently used graph
        bindingsForAnnotation.add(GRAPH, ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        // annotated for the current question
        bindingsForAnnotation.add("source", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        // only for relevant annotations
        bindingsForAnnotation.add("filterStart", ResourceFactory.createTypedLiteral(String.valueOf(filterStart), XSDDatatype.XSDint));

        String sparqlGetAnnotation = this.loadQueryFromFile(FILENAME_ANNOTATIONS_FILTERED, bindingsForAnnotation);

        // STEP 3: Compute SPARQL select queries that should produce the result for every identified entity
        //
        // Rather than computing a (textual) result this component provides a
        // SPARQL query that might be used to answer the question.
        // This query can the used by other components.

        // there might be multiple entities identified for one name
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

    private ArrayList<String> createQueriesForAnnotation(ResultSet resultsetFirstname, ResultSet resultsetLastname) throws IOException {
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
        return this.loadQueryFromFile(FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME, bindingsForWikidataResultQuery);
    }
}
