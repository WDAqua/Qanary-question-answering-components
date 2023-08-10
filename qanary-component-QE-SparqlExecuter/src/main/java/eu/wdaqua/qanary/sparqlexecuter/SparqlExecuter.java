package eu.wdaqua.qanary.sparqlexecuter;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
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


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class SparqlExecuter extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(SparqlExecuter.class);

    private final String applicationName;

    public SparqlExecuter(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;
    }

    @Value("${knowledgegraph.endpoint.dbpedia}")
    private String knowledgegraphEndpointDbpedia;

    @Value("${knowledgegraph.endpoint.wikidata}")
    private String knowledgegraphEndpointWikidata;

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
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());

        // STEP 1: get the required data from the triplestore
        String sparqlQuery = getResultSparqlQuery(myQanaryUtils, myQanaryQuestion);

        // STEP 2: execute the first SPARQL query
        String endpoint = selectKnowledgeGraphEnpdoint(sparqlQuery);
        if (endpoint == null) {
            return myQanaryMessage;
        }

        String json = getQueryResultsAsJson(sparqlQuery, endpoint);
        logger.info("Generated answers in RDF json: {}", json);

        // STEP 3: Push the the JSON object to the named graph reserved for the question
        logger.info("Push the the JSON object to the named graph reserved for the answer");
        String sparqlInsertAnnotationOfAnswerJson = getSparqlInsertQuery(json, myQanaryQuestion);
        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparqlInsertAnnotationOfAnswerJson);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlInsertAnnotationOfAnswerJson);

        return myQanaryMessage;
    }

    /**
     * @param sparqlQuery
     * @param endpoint
     * @return
     * @throws UnsupportedEncodingException
     * @throws SparqlQueryFailed
     * @throws URISyntaxException
     */
    public String getQueryResultsAsJson(String sparqlQuery, String endpoint) throws UnsupportedEncodingException, eu.wdaqua.qanary.sparqlexecuter.exception.SparqlQueryFailed, URISyntaxException{
        Query query = QueryFactory.create(sparqlQuery);
        TripleStoreConnector myTripleStoreConnector = new TripleStoreConnector(new URI(endpoint));
        String json;
        ByteArrayOutputStream outputStream;
        if (query.isAskType()) {
            Boolean result = myTripleStoreConnector.ask(sparqlQuery);
            outputStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outputStream, result);
        } else {
            ResultSet result = myTripleStoreConnector.select(sparqlQuery);
            outputStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outputStream, result);
        }
        json = new String(outputStream.toByteArray(), "UTF-8");
        return json;
    }
    
    public String selectKnowledgeGraphEnpdoint(String sparqlQuery){
        // TODO: extend functionality to use qa:TargetDataset if present
        String endpoint = "";
        if (sparqlQuery.contains("http://dbpedia.org")) {
            endpoint = this.knowledgegraphEndpointDbpedia;
            logger.info("use DBpedia endpoint");
        } else if (sparqlQuery.contains("http://www.wikidata.org")) {
            endpoint = this.knowledgegraphEndpointWikidata;
            logger.info("use Wikidata endpoint");
        } else {
            logger.warn("knowledge graph was unknown");
            return null;
        }
        return endpoint;
    }

    public String getResultSparqlQuery(QanaryUtils myQanaryUtils, QanaryQuestion myQanaryQuestion) throws SparqlQueryFailed, IOException{
        // TODO: this was changed (no longer using QanaryMessage.getInGraph())
        ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(QanaryTripleStoreConnector.getHighestScoreAnnotationOfAnswerInGraph(myQanaryQuestion.getOutGraph()));
        String sparqlQuery = "";
        while (resultset.hasNext()) {
            sparqlQuery = resultset.next().get("selectQueryThatShouldComputeTheAnswer").toString().replace("\\\"", "\"").replace("\\n", "\n");
        }
        logger.info("Generated SPARQL query: {} ", sparqlQuery);
        if(resultset.getRowNumber() > 1) {
        	logger.warn("There are {} SPARQL queries that might represent the correct answer. Just the first one is used.", resultset.getRowNumber());
        }
        return sparqlQuery;
    }

    public String getSparqlInsertQuery(String json, QanaryQuestion myQanaryQuestion) throws IOException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed{
        // define here the parameters for the SPARQL INSERT query
        QuerySolutionMap bindings = new QuerySolutionMap();
        // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        // TODO: check string replacement => leads to failed parsing in other components!
        //bindings.add("jsonAnswer", ResourceFactory.createTypedLiteral(json.replace("\n", "").replace("\"", "\\\""), XSDstring));
        bindings.add("jsonAnswer", ResourceFactory.createTypedLiteral(json.replace("\n", ""), XSDstring));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparqlInsertAnnotationOfAnswerJson = QanaryTripleStoreConnector.insertAnnotationOfAnswerJson(bindings);

        return sparqlInsertAnnotationOfAnswerJson;
    }

}
