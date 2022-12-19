package eu.wdaqua.qanary.sparqlexecuter;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;


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
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        URI myQuestionUri = myQanaryQuestion.getUri();
        // TODO: implement processing of question

        ResultSet resultset = myQanaryUtils.getQanaryTripleStoreConnector().select(QanaryTripleStoreConnector.getHighestScoreAnnotationOfAnswerInGraph(myQanaryMessage.getInGraph()));
        String sparqlQuery = "";
        while (resultset.hasNext()) {
            sparqlQuery = resultset.next().get("selectQueryThatShouldComputeTheAnswer").toString().replace("\\\\\"", "\"").replace("\\n", "\n");
        }
        logger.info("Generated SPARQL query: {} ", sparqlQuery);
        // STEP 2: execute the first sparql query

        String endpoint = "";
        if (sparqlQuery.contains("http://dbpedia.org")) {
            endpoint = this.knowledgegraphEndpointDbpedia;
            logger.info("use DBpedia endpoint");
        } else if (sparqlQuery.contains("http://www.wikidata.org")) {
            endpoint = this.knowledgegraphEndpointWikidata;
            logger.info("use Wikidata endpoint");
        } else {
            logger.warn("knowledge graph was unknown");
            return myQanaryMessage;
        }
        // @TODO: extend functionality to use qa:TargetDataset if present

        Query query = QueryFactory.create(sparqlQuery);
        TripleStoreConnector TripleStoreConnector = new TripleStoreConnector(new URI(endpoint));
        String json;
        if (query.isAskType()) {
            Boolean result = TripleStoreConnector.ask(sparqlQuery);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outputStream, result);
            json = new String(outputStream.toByteArray(), "UTF-8");
        } else {
            ResultSet result = TripleStoreConnector.select(sparqlQuery);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outputStream, result);
            json = new String(outputStream.toByteArray(), "UTF-8");
        }
        logger.info("Generated answers in RDF json: {}", json);

        // STEP 3: Push the the json object to the named graph reserved for the question
        logger.info("Push the the JSON object to the named graph reserved for the answer");

        // define here the parameters for the SPARQL INSERT query
        QuerySolutionMap bindings = new QuerySolutionMap();
        // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindings.add("jsonAnswer", ResourceFactory.createTypedLiteral(json.replace("\n", " ").replace("\"", "\\\""), XSDstring));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparqlInsertAnnotationOfAnswerJson = QanaryTripleStoreConnector.insertAnnotationOfAnswerJson(bindings);
        logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparqlInsertAnnotationOfAnswerJson);

        myQanaryUtils.getQanaryTripleStoreConnector().update(sparqlInsertAnnotationOfAnswerJson);


        return myQanaryMessage;
    }

}
