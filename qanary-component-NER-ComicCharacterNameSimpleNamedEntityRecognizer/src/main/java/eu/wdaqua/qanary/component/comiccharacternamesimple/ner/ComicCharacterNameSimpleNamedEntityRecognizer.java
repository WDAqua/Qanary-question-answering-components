package eu.wdaqua.qanary.component.comiccharacternamesimple.ner;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
/**
 * This Qanary component is annotating DBpedia's Superhero entities within
 * questions. Example: "What is the real name of Catwoman?" with the entity
 * "Catwoman" as it is a Superhero name found in the DBpedia triplestore.
 *
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 */
public class ComicCharacterNameSimpleNamedEntityRecognizer extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(ComicCharacterNameSimpleNamedEntityRecognizer.class);

    private final String applicationName;
    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";
    private String FILENAME_SELECT_HEROS = "/queries/select_all_superhero.rq";

    public ComicCharacterNameSimpleNamedEntityRecognizer(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_SELECT_HEROS);
    }

    /**
     * try to find a superhero name in the given question using a trivial string
     * matching for entity recognition the label of the entities are fetched from
     * DBpedia every time
     *
     * @throws Exception
     */
    public SuperheroNamedEntityFound getAllSuperheroNamesFromDBpediaMatchingPositions(String question) throws IOException {

        // query DBpedia for all superhero film characters
        String serviceUrl = "http://dbpedia.org/sparql";

        QuerySolutionMap bindingsForSelect = new QuerySolutionMap();

        // get the template of the query
        String sparql = this.loadQueryFromFile(FILENAME_SELECT_HEROS, bindingsForSelect);

        logger.info("searching for character names on DBpedia ...\nDBpedia query: \n{}", sparql);

        ResultSet rs = this.selectFromCostumeTripleStore(sparql, serviceUrl);
        while (rs.hasNext()) {
            QuerySolution s = rs.nextSolution();
            String characterName = this.getCharacterName(s);
            if (nameFound(question, characterName)) {
                logger.info("found Super Hero name: {}", characterName);
                String resource = this.getResource(s);
                int[] index = this.getIndexOfName(question, characterName);
                // return the found entity
                return new SuperheroNamedEntityFound(characterName, resource, index[0], index[1]); //note that it will currently stop after having found just one matching name
            }
        }
        // if nothing was found, then return null
        logger.warn("no matching names could be found");
        return null;
    }

    private boolean nameFound(String question, String name) {
        String q = question.toLowerCase();
        String n = name.toLowerCase().trim();
        return q.contains(n);
    }

    /**
     * replace known bracketed extension of superhero names known to be added in
     * Wikipedia, e.g., Doctor Doom (Comic) -> Doctor Doom, Daredevil (superheld) ->
     * Daredevil, Spawn (personaggio) -> Spawn
     *
     * @param solution
     * @return
     */
    private String getCharacterName(QuerySolution solution) {
        return solution.getLiteral("herolabelString").toString().replaceAll("\\(.*\\)", "");
    }

    private String getResource(QuerySolution solution) {
        return solution.getResource("hero").toString();
    }

    private int[] getIndexOfName(String question, String name) {
        int[] nameIndex = new int[2];
        name = name.trim();
        nameIndex[0] = question.indexOf(name);
        nameIndex[1] = question.indexOf(name) + name.length();
        return nameIndex;
    }

    /**
     * process the provided message from the Qanary pipeline this method is the
     * entry point of this Qanary component
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws IOException {
        logger.info("process: {}", myQanaryMessage);

        // fetching question from database
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String triplestore = myQanaryQuestion.getEndpoint().toString();

        String question = null;
        try {
            question = myQanaryQuestion.getTextualRepresentation();
        } catch (Exception e) {
            // if the textual representation of a question could not be retrieved, then stop
            // the execution
            logger.error("Unable to get textual representation of question:\n{}", ExceptionUtils.getStackTrace(e));
            return myQanaryMessage;
        }

        // try to find a superhero name in the question
        SuperheroNamedEntityFound foundSuperhero = getAllSuperheroNamesFromDBpediaMatchingPositions(question);
        if (foundSuperhero == null) {
            logger.warn("No annotation added to Qanary triplestore.");
            return myQanaryMessage;
        }

        try {
            // create SPARQL query to insert annotation into triple store
            logger.info("inserting annotation with start: {}, end: {}", //
                    foundSuperhero.getBeginIndex(), foundSuperhero.getEndIndex());

            String sparql = this.getSparqlInsertQuery(foundSuperhero, myQanaryQuestion);
            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        } catch (SparqlQueryFailed e) {
            logger.error("SPARQL query to insert data into Qanary triplestore {} failed.\n{}", //
                    e.getTriplestore(), ExceptionUtils.getStackTrace(e));
            return myQanaryMessage;
        } catch (URISyntaxException e) {
            logger.error("Qanary message did not contain proper URIs.\n{}", //
                    ExceptionUtils.getStackTrace(e));
            return myQanaryMessage;
        } catch (QanaryExceptionNoOrMultipleQuestions e) {
            logger.error("Given graph did not contain a question of the user.\n{}", //
                    ExceptionUtils.getStackTrace(e));
            return myQanaryMessage;
        } catch (IOException e) {
            logger.error("Could not load SPARQL query from file.\n{}", //
                    ExceptionUtils.getStackTrace(e));
            return myQanaryMessage;
        }

        logger.info("Component processing finished successfully. Annotation of {} inserted into Qanary triplestore {}.",
                foundSuperhero.getSuperheroLabel(), triplestore);
        return myQanaryMessage;
    }

    public String getSparqlInsertQuery(SuperheroNamedEntityFound foundSuperhero, QanaryQuestion<String> myQanaryQuestion) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(foundSuperhero.getBeginIndex()), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(foundSuperhero.getEndIndex()), XSDDatatype.XSDnonNegativeInteger));
        bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
        logger.info("SPARQL query: {}", sparql);

        return sparql;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    private ResultSet selectFromCostumeTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }
}
