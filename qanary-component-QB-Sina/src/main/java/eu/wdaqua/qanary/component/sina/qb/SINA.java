package eu.wdaqua.qanary.component.sina.qb;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URISyntaxException;


@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class SINA extends QanaryComponent {

    private static final Logger logger = LoggerFactory.getLogger(SINA.class);
    private String sinaJarFileLocation;

    @Value("${spring.application.name}")
    private String applicationName;

    private String FILENAME_GET_ENTITIES = "/queries/select_all_AnnotationOfInstance.rq";
    private String FILENAME_GET_RELATIONS = "/queries/get_relations.rq";
    private String FILENAME_GET_CLASSES = "/queries/get_classes.rq";
    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public SINA(@Value("${sina.jarfilelocation}") String sinaJarFileLocation) throws IOException, InterruptedException {
        logger.info("sina.jarfilelocation: {}", sinaJarFileLocation);

        this.sinaJarFileLocation = sinaJarFileLocation;
        //this.executeExternalSinaJarFile("http://dbpedia.org/resource/Berlin");

        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_ENTITIES);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_RELATIONS);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_CLASSES);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);

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

        QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> qanaryQuestion = new QanaryQuestion<>(myQanaryMessage, qanaryUtils.getQanaryTripleStoreConnector());
        String myQuestion = qanaryQuestion.getTextualRepresentation();
        logger.info("myQuestion: {}", myQuestion);

        final String argument = fetchEntitiesRelationsAndClasses(qanaryQuestion, qanaryUtils);

        logger.info("Sina Arguments: {}: {}", argument, argument.length());
        logger.info("Sina Argument Count: {}", StringUtils.countMatches(argument, "dbpedia"));

        if (argument.length() > 2 && StringUtils.countMatches(argument, "dbpedia") <= 3) {
            final String endpoint = myQanaryMessage.getEndpoint().toString();
            final String[] queryTemplates = runSina(argument);
//            final String questionUri = getQuestionURI(qanaryUtils, myQanaryMessage.getInGraph().toString(), endpoint);
//            final String updateQuery = createUpdateQueryFromQueryTemplate(queryTemplates, qanaryUtils, questionUri);

            int x = 10;
            for (int i = 0; i < queryTemplates.length; i++) {

                QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
                bindingsForInsert.add("graph", ResourceFactory.createResource(qanaryQuestion.getOutGraph().toASCIIString()));
                bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(qanaryQuestion.getUri().toASCIIString()));
                bindingsForInsert.add("body", ResourceFactory.createTypedLiteral(queryTemplates[i], XSDDatatype.XSDstring));
                bindingsForInsert.add("score", ResourceFactory.createTypedLiteral(String.valueOf(x--), XSDDatatype.XSDfloat));
                bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

                // get the template of the INSERT query
                String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
                logger.info("SPARQL query: {}", sparql);
                qanaryUtils.getQanaryTripleStoreConnector().update(sparql);
            }
        } else {
            logger.info("Argument is Null {}", argument);
        }
        return myQanaryMessage;
    }

    private StringBuilder fetchEntities(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, IOException {
        QuerySolutionMap bindingsForGetEntities = new QuerySolutionMap();
        bindingsForGetEntities.add("graph", ResourceFactory.createResource(qanaryQuestion.getInGraph().toASCIIString()));

        String sparqlGetEntities = this.loadQueryFromFile(FILENAME_GET_ENTITIES, bindingsForGetEntities);
        logger.info("fetchEntities for given question with query: {}", sparqlGetEntities);
        final ResultSet entitiesResultSet = qanaryUtils.getQanaryTripleStoreConnector().select(sparqlGetEntities);

        final StringBuilder argument = new StringBuilder();
        while (entitiesResultSet.hasNext()) {
            QuerySolution s = entitiesResultSet.next();

            final Entity entity = new Entity(s.getResource("hasBody").getURI(), s.getLiteral("start").getInt(), s.getLiteral("end").getInt());
            argument.append(entity.uri + ", ");

            logger.info("uri:{} start:{} end:{}", entity.uri, entity.begin, entity.end);
        }
        return argument;
    }

    private StringBuilder fetchRelations(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException {
        QuerySolutionMap bindingsForGetRelations = new QuerySolutionMap();
        bindingsForGetRelations.add("graph", ResourceFactory.createResource(qanaryQuestion.getInGraph().toASCIIString()));
        bindingsForGetRelations.add("targetQuestion", ResourceFactory.createResource(qanaryQuestion.getUri().toASCIIString()));

        String sparqlGetRelations = this.loadQueryFromFile(FILENAME_GET_RELATIONS, bindingsForGetRelations);
        logger.info("fetchRelations for given question with query {}", sparqlGetRelations);
        final ResultSet relationResultSet = qanaryUtils.getQanaryTripleStoreConnector().select(sparqlGetRelations);

        final StringBuilder argument = new StringBuilder();
        while (relationResultSet.hasNext()) {
            QuerySolution s = relationResultSet.next();

            final Entity entity = new Entity(s.getResource("relationurl").getURI());
            // entityTemp2.begin = s.getLiteral("start").getInt();
            // entityTemp2.end = s.getLiteral("end").getInt();

            argument.append(entity.uri + ", ");

            logger.info("uri info {}", entity.uri);
        }
        return argument;
    }

    private StringBuilder fetchClasses(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException {
        QuerySolutionMap bindingsForGetClasses = new QuerySolutionMap();
        bindingsForGetClasses.add("graph", ResourceFactory.createResource(qanaryQuestion.getInGraph().toASCIIString()));
        bindingsForGetClasses.add("targetQuestion", ResourceFactory.createResource(qanaryQuestion.getUri().toASCIIString()));

        String sparqlGetClasses = this.loadQueryFromFile(FILENAME_GET_CLASSES, bindingsForGetClasses);
        logger.info("fetchClasses for given question with query {}", sparqlGetClasses);
        final ResultSet classResultSet = qanaryUtils.getQanaryTripleStoreConnector().select(sparqlGetClasses);
        final StringBuilder argument = new StringBuilder();
        while (classResultSet.hasNext()) {
            QuerySolution s = classResultSet.next();
            Entity entityTemp3 = new Entity(s.getResource("uri").getURI());
            argument.append(entityTemp3.uri + ", ");
            logger.info("uri info {}", s.getResource("uri").getURI());
        }
        return argument;
    }

    private String fetchEntitiesRelationsAndClasses(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, IOException {
        final StringBuilder argument = fetchEntities(qanaryQuestion, qanaryUtils);
        argument.append(fetchRelations(qanaryQuestion, qanaryUtils));
        argument.append(fetchClasses(qanaryQuestion, qanaryUtils));
        return argument.substring(0, argument.length() - 2);
    }

    /**
     * execute SINA JAR file and retrieve an array of query candidates
     *
     * @param argument
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    protected String[] runSina(String argument) throws IOException, InterruptedException {
        String queryCandidates = executeExternalSinaJarFile(argument);
        queryCandidates = queryCandidates.trim();
        queryCandidates = queryCandidates.substring(1, queryCandidates.length() - 1);

        return queryCandidates.split(",");
    }

    /**
     * get queryTemplates via executing the SINA JAR file as external process
     *
     * @param argument
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    protected String executeExternalSinaJarFile(String argument) throws IOException, InterruptedException {
        String validSinaJarFileAbsoluteLocation = this.getValidSinaJarFileAbsoluteLocation(sinaJarFileLocation);
        logger.info("Path to SINA JAR file: {}", validSinaJarFileAbsoluteLocation);

        logger.info("executeExternalSinaJarFile: argument={} on {}", argument, validSinaJarFileAbsoluteLocation);

        final ProcessBuilder pb = new ProcessBuilder("java", "-jar", validSinaJarFileAbsoluteLocation, argument);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        p.waitFor();

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String outputRetrieved = "";
        String line;
        while ((line = br.readLine()) != null) {
            outputRetrieved += line;
        }
        br.close();
        p.destroy();

        removeSinaTempFile(validSinaJarFileAbsoluteLocation);

        logger.debug("executeExternalSinaJarFile: retrieved output={}", outputRetrieved);

        String queryCandidates = outputRetrieved.substring(outputRetrieved.indexOf("list of final templates:") + "list of final templates:".length());

        logger.info("Found query candidates: {}", queryCandidates);


        return queryCandidates;
    }

    /**
     * computes the absolute SINA JAR file location and checks if the file is present there
     *
     * @param sinaJarFileName
     * @return
     * @throws IOException
     */
    protected String getValidSinaJarFileAbsoluteLocation(String sinaJarFileName) throws IOException {
        if (sinaJarFileName == null || sinaJarFileName.isEmpty()) {
            throw new NoSinaFileProvidedException();
        }

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:" + sinaJarFileName);
        String sinaJarFileAbsoluteLocation = "";

        for (Resource r : resources) {
            InputStream inputStream = r.getInputStream();
            File somethingFile = File.createTempFile(r.getFilename(), ".cxl");
            try {
                FileUtils.copyInputStreamToFile(inputStream, somethingFile);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
            logger.info("File Path is {}", somethingFile.getAbsolutePath());
            sinaJarFileAbsoluteLocation = somethingFile.getAbsolutePath();
        }

        if (!(new File(sinaJarFileAbsoluteLocation).exists())) {
            throw new NoSinaFileNotFoundException(sinaJarFileAbsoluteLocation);
        }

        logger.info("Found JAR file ({}) at {}", sinaJarFileName, sinaJarFileAbsoluteLocation);
        return sinaJarFileAbsoluteLocation;
    }

    protected void removeSinaTempFile(String sinaJarFile) {
        File file = new File(sinaJarFile);
        file.delete();

        logger.info("Removed SINA temp file: {}", sinaJarFile);
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    protected class Entity {
        public final String uri;
        public int begin;
        public int end;
        public String namedEntity;

        Entity(String uri) {
            this.uri = uri;
        }

        Entity(String uri, int begin, int end) {
            this.uri = uri;
            this.begin = begin;
            this.end = end;
        }
    }
}
