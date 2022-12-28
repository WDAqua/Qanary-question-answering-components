package eu.wdaqua.component.birthdatawikidata.qb;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import io.swagger.v3.oas.annotations.Operation;
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

    private final String applicationName;

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
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        String myQuestion = myQanaryQuestion.getTextualRepresentation();

        // This component is only supposed to answer a specific type of question.
        // Therefore, we only need to continue if the question asks for birthplace and date.
        // For this example is enough to match the question against a simple regular expression.
        // However, a more sophisticated approach is possible.

        if (!this.isQuestionSupported(myQuestion)) {
            // don't continue the process if the question is not supported
            logger.info("nothing to do here as quest-on \"{}\" does not have the supported format",
                    myQuestion);
            return myQanaryMessage;
        }

        // STEP 2: Get Wikidata entities that were annotated by an NED/NER component.
        //
        // In this example we are only interested in Entities that were found at a specific point
        // in the question: e.g. 'when and where was <name> born?'.
        // Because we do not require entities that might have been found anywhere else in the
        // question we can filter our results:

        int filterStart = this.getNamePosition(myQuestion, this.supportedQuestionPatterns[this.patternIndex]);
        // formulate a query to find existing information

        QuerySolutionMap bindingsForAnnotation = new QuerySolutionMap();
        // the currently used graph
        bindingsForAnnotation.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        // annotated for the current question
        bindingsForAnnotation.add("source", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        // only for relevant annotations
        bindingsForAnnotation.add("filterStart", ResourceFactory.createTypedLiteral(String.valueOf(filterStart), XSDDatatype.XSDint));

        String sparqlGetAnnotation = this.loadQueryFromFile("/queries/getAnnotation.rq", bindingsForAnnotation);

        // STEP 3: Compute SPARQL select queries that should produce the result for every identified entity
        //
        // Rather than computing a (textual) result this component provides a
        // SPARQL query that might be used to answer the question.
        // This query can the used by other components.

        // there might be multiple entities identified for one name
        ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(sparqlGetAnnotation);
        while (resultset.hasNext()) {
            QuerySolution tupel = resultset.next();
            String wikidataResource = tupel.get("wikidataResource").toString();
            logger.info("creating query for resource: {}", wikidataResource);

            // populate a generalized answer query with the specific entity (wikidata ID)
            QuerySolutionMap bindingsForWikiDataQuery = new QuerySolutionMap();
            bindingsForWikiDataQuery.add("wikidataResource", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));

            String createdWikiDataQuery = "" //
                    + "PREFIX wikibase: <http://wikiba.se/ontology#> " //
                    + "PREFIX wd: <http://www.wikidata.org/entity/> " //
                    + "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " //
                    + "PREFIX bd: <http://www.bigdata.com/rdf#> " //
                    + "PREFIX p: <http://www.wikidata.org/prop/> " //
                    + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/> " //
                    + "PREFIX ps: <http://www.wikidata.org/prop/statement/> " //
                    + "select DISTINCT ?personLabel ?birthplaceLabel ?birthdate " //
                    + "where { " //
                    + "	 values ?allowedPropPlace { pq:P17 } " // allow 'country' as property of birthplace
                    + "  values ?person {<" + wikidataResource + ">} " //
                    + "  ?person wdt:P569 ?birthdate . " // this should produce the date of birth
                    + "  {" //
                    + "  ?person wdt:P19 ?birthplace . " // this should produce the place of birth
                    + "  }" //
                    + "	 UNION" //
                    + "  {" //
                    + "  ?person wdt:P19 ?specificBirthPlace . " //
                    + "  ?person p:P19 _:a . " //
                    + "  _:a ps:P19 ?specificBirthPlace . " // the above place might be too specific
                    + "  _:a ?allowedPropPlace ?birthplace . "// get the country if it is provided
                    + "  }" //
                    + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" } " //
                    + "}";

            // store the created select query as an annotation for the current question
            // define here the parameters for the SPARQL INSERT query
            QuerySolutionMap bindings = new QuerySolutionMap();
            // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
            bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindings.add("selectQueryThatShouldComputeTheAnswer", ResourceFactory.createStringLiteral(createdWikiDataQuery.replace("\"", "\\\"").replace("\n", "\\n")));
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
}
