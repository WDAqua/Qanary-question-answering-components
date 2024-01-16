package eu.wdaqua.qanary.component.simplequerybuilderandexecutor.qbe;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class QueryBuilder extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
    private final String applicationName;
    @Value("${dbpedia.sparql.endpoint:http://dbpedia.org/sparql}")
    String dbpediaSparqlEndpoint;

    private String FILENAME_SELECT_CLASSES = "/queries/select_all_AnnotationOfClass.rq";
    private String FILENAME_SELECT_PROPERTIES = "/queries/select_all_AnnotationOfRelation.rq";
    private String FILENAME_SELECT_ENTITIES = "/queries/select_all_AnnotationOfInstance.rq";
    private String FILENAME_INSERT_SPARQL = "/queries/insert_sparql.rq";
    private String FILENAME_INSERT_JSON = "/queries/insert_json.rq";

    public QueryBuilder(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_CLASSES);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_PROPERTIES);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_ENTITIES);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_SPARQL);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_JSON);
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
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        logger.info("Question: {}", myQuestion);

        String sparql;

        // random answer URI
        String answerID = "urn:qanary:answer:" + UUID.randomUUID().toString();

        // get entities, properties, classes from current question
        List<String> entities = getEntitiesFromQanaryKB(myQanaryUtils, myQanaryQuestion);
        List<String> properties = getPropertiesFromQanaryKB(myQanaryUtils, myQanaryQuestion);
        List<String> classes = getClassesFromQanaryKB(myQanaryUtils, myQanaryQuestion);

        String generatedQuery = "";
        if (classes.size() == 0) {

            if (properties.size() == 1) {
                if (entities.size() == 1) {

                    if (myQuestion.contains("Is") || myQuestion.contains("Did") || myQuestion.contains("do")
                            || myQuestion.contains("Does") || myQuestion.contains("Was") || myQuestion.contains("Were")
                            || myQuestion.contains("Are")) {
                        generatedQuery = "ASK WHERE { " //
                                + "<" + entities.get(0) + "> <" + properties.get(0) + "> ?v1 . " //
                                + "}";
                    } else if (myQuestion.contains("How") || myQuestion.contains("many")) {
                        generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE{ " //
                                + "<" + entities.get(0) + "> <" + properties.get(0) + "> ?v1 . " //
                                + "}";

                    } else {
                        generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                                + "<" + entities.get(0) + "> <" + properties.get(0) + "> ?uri ." //
                                + "}";
                    }
                }
                if (entities.size() == 2) {

                    if (myQuestion.contains("Is") || myQuestion.contains("Did") || myQuestion.contains("do")
                            || myQuestion.contains("Does") || myQuestion.contains("Was") || myQuestion.contains("Were")
                            || myQuestion.contains("Are")) {
                        generatedQuery = "ASK WHERE { " //
                                + "<" + entities.get(0) + "> <" + properties.get(0) + "> <" + entities.get(1) + "> ." //
                                + "}";
                    }
                    if (myQuestion.contains("both") || myQuestion.contains("common") || myQuestion.contains("also")) {

                        generatedQuery = "SELECT DISTINCT ?uri WHERE {"//
                                + " ?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                                + " ?uri <" + properties.get(0) + "> <" + entities.get(1) + "> . " //
                                + "}";
                    }
                }
            } else if (properties.size() == 2) {
                if (entities.size() == 1) {

                    if (myQuestion.contains("How") || myQuestion.contains("many")) {

                        generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE { " //
                                + "?x <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                                + "?x <" + properties.get(1) + "> ?uri . " //
                                + "}";

                        Query query = QueryFactory.create(generatedQuery);
                        QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqlEndpoint, query);
                        ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

                        if (!results.hasNext()) {
                            generatedQuery = "SELECT (COUNT(DISTINCT ?x) as ?c) WHERE { " //
                                    + "<" + entities.get(0) + "> <" + properties.get(0) + "> ?x . " //
                                    + "?x <" + properties.get(1) + "> ?uri . " //
                                    + "}";
                        }

                    } else {
                        generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                                + "?x <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                                + "?x <" + properties.get(1) + "> ?uri . "//
                                + "}";

                        Query query = QueryFactory.create(generatedQuery);
                        QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqlEndpoint, query);
                        ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
                        if (!results.hasNext()) {
                            generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                                    + "<" + entities.get(0) + "> <" + properties.get(0) + "> ?x . " //
                                    + "?x <" + properties.get(1) + "> ?uri . " //
                                    + "}";
                        }
                    }
                }
                if (entities.size() == 2) {

                    generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                            + "?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                            + "?uri <" + properties.get(1) + "> <" + entities.get(1) + "> . }";
                }
            }

        } else if (classes.size() == 1) {

            if (properties.size() == 0) {
                if (entities.size() == 1) {
                    // TODO
                } else if (entities.size() == 2) {
                    // TODO
                }
            } else if (properties.size() == 1) {
                if (entities.size() == 1) {

                    generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                            + "?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                            + "?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
                            + "}";

                    Query query = QueryFactory.create(generatedQuery);
                    QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqlEndpoint, query);
                    ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
                    if (!results.hasNext()) {
                        generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                                + "<" + entities.get(0) + "> <" + properties.get(0) + "> ?uri . " //
                                + "?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
                                + "}";
                    }

                }
                if (entities.size() == 2) {
                    // TODO
                }
            } else if (properties.size() == 2) {
                if (entities.size() == 1) {
                    generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                            + "?x <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                            + "?x <" + properties.get(1) + "> ?uri . " //
                            + "?x <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
                            + "}";
                }
                if (entities.size() == 2) {
                    generatedQuery = "SELECT DISTINCT ?uri WHERE { " //
                            + "?uri <" + properties.get(0) + "> <" + entities.get(0) + "> . " //
                            + "?uri <" + properties.get(1) + "> <" + entities.get(1) + "> . " //
                            + "?uri <https://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + classes.get(0) + "> . " //
                            + "}";
                }
            }

        } else if (classes.size() == 2) { // TODO

            if (properties.size() == 0) {
                if (entities.size() == 1) {

                }
                if (entities.size() == 2) {

                }
            } else if (properties.size() == 1) {
                if (entities.size() == 1) {

                }
                if (entities.size() == 2) {

                }
            } else if (properties.size() == 2) {
                if (entities.size() == 1) {

                }
                if (entities.size() == 2) {

                }
            }
        }

        logger.debug("store the generated SPARQL query in triplestore: {}", generatedQuery);
        // STEP 3: Push the SPARQL query to the triplestore
        if (generatedQuery != "") {
            QuerySolutionMap bindingsForInsertSparql = new QuerySolutionMap();
            bindingsForInsertSparql.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsertSparql.add("body", ResourceFactory.createStringLiteral(generatedQuery));
            bindingsForInsertSparql.add("answer", ResourceFactory.createResource(answerID));
            bindingsForInsertSparql.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            sparql = this.loadQueryFromFile(FILENAME_INSERT_SPARQL, bindingsForInsertSparql);
            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

            Query query = QueryFactory.create(generatedQuery);
            QueryExecution exec = QueryExecutionFactory.sparqlService(dbpediaSparqlEndpoint, query);

            ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ResultSetFormatter.outputAsJSON(outputStream, results);
            String json = new String(outputStream.toByteArray(), "UTF-8");

            logger.info("Push the the JSON object to the named graph reserved for the answer.");

            QuerySolutionMap bindingsForInsertJson = new QuerySolutionMap();
            bindingsForInsertJson.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsertJson.add("body", ResourceFactory.createStringLiteral(json));
            bindingsForInsertJson.add("answer", ResourceFactory.createResource(answerID));
            bindingsForInsertJson.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            sparql = this.loadQueryFromFile(FILENAME_INSERT_JSON, bindingsForInsertSparql);
            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        }
        return myQanaryMessage;
    }

    /**
     * get all annotated classes of the user's question from the Qanary
     * Knowledge Base / Triplestore
     *
     * @param myQanaryUtils
     * @param myQanaryQuestion
     * @return
     */
    private List<String> getClassesFromQanaryKB(QanaryUtils myQanaryUtils, QanaryQuestion<String> myQanaryQuestion) throws SparqlQueryFailed, IOException {
        List<String> classes = new ArrayList<String>();

        QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
        bindingsForSelect.add("graph", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));

        // get the template of the select query
        String sparql = this.loadQueryFromFile(FILENAME_SELECT_CLASSES, bindingsForSelect);

        ResultSet r = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);

        while (r.hasNext()) {
            QuerySolution s = r.next();
            classes.add(s.getResource("hasBody").getURI());
            logger.info("class: {}", s.getResource("hasBody").getURI());
        }

        return classes;
    }

    /**
     * get all annotated properties of the user's question from the Qanary
     * Knowledge Base / Triplestore
     *
     * @param myQanaryUtils
     * @param myQanaryQuestion
     * @return
     */
    private List<String> getPropertiesFromQanaryKB(QanaryUtils myQanaryUtils, QanaryQuestion<String> myQanaryQuestion) throws SparqlQueryFailed, IOException {
        List<String> properties = new ArrayList<String>();

        QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
        bindingsForSelect.add("graph", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));

        // get the template of the select query
        String sparql = this.loadQueryFromFile(FILENAME_SELECT_PROPERTIES, bindingsForSelect);

        ResultSet r = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);

        while (r.hasNext()) {
            QuerySolution s = r.next();
            properties.add(s.getResource("hasBody").getURI());
            logger.info("property: {}", s.getResource("hasBody").getURI());
        }
        return properties;
    }

    /**
     * get all annotated entities of the user's question from the Qanary
     * Knowledge Base / Triplestore
     *
     * @param myQanaryUtils
     * @param myQanaryQuestion
     * @return
     */
    private List<String> getEntitiesFromQanaryKB(QanaryUtils myQanaryUtils, QanaryQuestion<String> myQanaryQuestion) throws SparqlQueryFailed, IOException {
        List<String> entities = new ArrayList<String>();

        QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
        bindingsForSelect.add("graph", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));

        // get the template of the select query
        String sparql = this.loadQueryFromFile(FILENAME_SELECT_ENTITIES, bindingsForSelect);

        ResultSet r = myQanaryUtils.getQanaryTripleStoreConnector().select(sparql);
        while (r.hasNext()) {
            QuerySolution s = r.next();

            entities.add(s.getResource("hasBody").getURI());
            logger.info("entity: {}", s.getResource("hasBody").getURI());
        }
        return entities;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }
}
