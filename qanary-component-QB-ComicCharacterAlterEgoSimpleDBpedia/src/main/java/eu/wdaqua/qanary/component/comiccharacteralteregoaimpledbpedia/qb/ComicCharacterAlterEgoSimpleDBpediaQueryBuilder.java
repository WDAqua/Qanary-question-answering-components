package eu.wdaqua.qanary.component.comiccharacteralteregoaimpledbpedia.qb;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
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

@Component
/**
 * This component creates a SPARQL query that can be used to find the real name of fictional characters.
 * It recognizes the question prefix "what is the real name of".
 * Example names:
 * "Iron Man"
 * "Catwoman"
 * "Rogue"
 * "Apocalypse"
 * "Daredevil"
 *
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class ComicCharacterAlterEgoSimpleDBpediaQueryBuilder extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(ComicCharacterAlterEgoSimpleDBpediaQueryBuilder.class);

    private final String applicationName;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";
    private String FILENAME_SELECT_ANNOTATION = "/queries/select_annotation.rq";
    private String FILENAME_DBPEDIA_QUERY = "/queries/dbpedia_query.rq";

    public ComicCharacterAlterEgoSimpleDBpediaQueryBuilder(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_ANNOTATION);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_DBPEDIA_QUERY);
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

        //read question from database
        QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> qanaryQuestion = new QanaryQuestion<>(myQanaryMessage, qanaryUtils.getQanaryTripleStoreConnector());
        String question = qanaryQuestion.getTextualRepresentation();

        String prefix = "what is the real name of";

        //return if question does not start with supported prefix
        if (!question.toLowerCase().startsWith(prefix)) {
            logger.info("Question \"{}\" does not start with \"{}\" - aborting process...", question, prefix);
            return myQanaryMessage;
        }

        QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
        bindingsForSelect.add("graph", ResourceFactory.createResource(qanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForSelect.add("targetQuestion", ResourceFactory.createResource(qanaryQuestion.getUri().toASCIIString()));

        // get the template of the INSERT query
        String sparql = this.loadQueryFromFile(FILENAME_SELECT_ANNOTATION, bindingsForSelect);
        logger.info("SPARQL query: {}", sparql);
        ResultSet resultSet = qanaryUtils.getQanaryTripleStoreConnector().select(sparql);

        if (!resultSet.hasNext()) {
            logger.warn("no matching resource could be found!");
        }

        while (resultSet.hasNext()) {
            QuerySolution result = resultSet.next();
            logger.info("result: \n{}", result);
            int start = result.get("startOfSpecificResource").asLiteral().getInt();
            int end = result.get("endOfSpecificResource").asLiteral().getInt();
            String name = question.substring(start, end);
            logger.warn("annotation found for name '{}' (at {},{})", name, start, end);

            QuerySolutionMap bindingsForDBpediaQeury = new QuerySolutionMap();
            bindingsForDBpediaQeury.add("name", ResourceFactory.createStringLiteral(name));

            // get the template of the INSERT query
            String dbpediaQuery = this.loadQueryFromFile(FILENAME_DBPEDIA_QUERY, bindingsForDBpediaQeury);
            logger.info("The answer might be computed via: \n{}", dbpediaQuery);

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(qanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(qanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("answer", ResourceFactory.createTypedLiteral(dbpediaQuery, XSDDatatype.XSDstring));
            bindingsForInsert.add("score", ResourceFactory.createTypedLiteral("1.0", XSDDatatype.XSDfloat));
            bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
            logger.info("SPARQL query: {}", sparql);
            qanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        }
        return myQanaryMessage;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

}
