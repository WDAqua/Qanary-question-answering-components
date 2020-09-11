package eu.wdaqua.qanary.sina;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


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

	public SINA(@Value("${sina.jarfilelocation}") String sinaJarFileLocation) throws IOException, InterruptedException {
		logger.info("sina.jarfilelocation: {}", sinaJarFileLocation);
		this.sinaJarFileLocation = this.getValidSinaJarFileAbsoluteLocation(sinaJarFileLocation);
		//this.executeExternalSinaJarFile("http://dbpedia.org/resource/Berlin");
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
		QanaryQuestion<String> qanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
		String myQuestion = qanaryQuestion.getTextualRepresentation();
		logger.info("myQuestion: {}", myQuestion);

		final String argument = fetchEntitiesRelationsAndClasses(qanaryQuestion, qanaryUtils);

		logger.info("Sina Arguments: {}: {}", argument, argument.length());
		logger.info("Sina Argument Count: {}",StringUtils.countMatches(argument, "dbpedia"));
		
		if(argument.length() > 2 && StringUtils.countMatches(argument, "dbpedia") <=3 ) {
			final String endpoint = myQanaryMessage.getEndpoint().toString();
			final String[] queryTemplates = runSina(argument);
			final String questionUri = getQuestionURI(qanaryUtils, myQanaryMessage.getInGraph().toString(), endpoint);
			final String updateQuery = createUpdateQueryFromQueryTemplate(queryTemplates, qanaryUtils, questionUri); 

			logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
			logger.info("apply vocabulary alignment on outgraph");
			qanaryUtils.updateTripleStore(updateQuery, endpoint);
		}
		else {
			logger.info("Argument is Null {}", argument);
		}
		return myQanaryMessage;
	}

	private StringBuilder fetchEntities(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed {
		final String sparql = "" // 
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "SELECT ?start ?end ?uri " //
							+ "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "    ?a a qa:AnnotationOfInstance . " // 
							+ "    ?a oa:hasTarget [ " //
							+ "		     a               oa:SpecificResource; " //
							+ "		     oa:hasSource    ?q; " //
							+ "	         oa:hasSelector  [ " //
							+ "			         a        oa:TextPositionSelector ; " //
							+ "			         oa:start ?start ; " //
							+ "			         oa:end   ?end " //
							+ "		     ] " //
							+ "    ] . " //
							+ " ?a oa:hasBody ?uri ; " //
							+ "} ";

		logger.info("fetchEntities for given question with query {}", sparql);

		final ResultSet entitiesResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		while (entitiesResultSet.hasNext()) {
			QuerySolution s = entitiesResultSet.next();

			final Entity entity = new Entity(s.getResource("uri").getURI(), s.getLiteral("start").getInt(), s.getLiteral("end").getInt());
			argument.append(entity.uri + ", ");

			logger.info("uri:{} start:{} end:{}", entity.uri, entity.begin, entity.end);
		}
		return argument;
	}

	private StringBuilder fetchRelations(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		final String sparql = "" // 
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " // 
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "SELECT ?relationurl " // 
							+ "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "  ?a a qa:AnnotationOfRelation . " //  
							+ "  ?a oa:hasTarget [ " //  
							+ "           a    oa:SpecificResource; " // 
							+ "           oa:hasSource    <" + qanaryQuestion.getUri() + ">; " // 
							+ "  ] ; " //   
							+ "     oa:hasBody ?relationurl ;" // 
							+ "	    oa:annotatedAt ?time  " // 
							+ "} " //
							+ "ORDER BY ?start ";

		logger.info("fetchRelations for given question with query {}", sparql);

		final ResultSet relationResultSet = qanaryUtils.selectFromTripleStore(sparql);
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

	private StringBuilder fetchClasses(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		final String sparql = "" //
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " // 
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "SELECT ?uri " //
							+ "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "  ?a a qa:AnnotationOfClass . " //  
							+ "  ?a oa:hasTarget [ " // 
							+ "           a    oa:SpecificResource; " // 
							+ "           oa:hasSource    <" + qanaryQuestion.getUri() + ">; " // 
							+ "  ] ; " //  
							+ "     oa:hasBody ?uri ;" //
							+ "	    oa:annotatedAt ?time  " //  
							+ "} " // 
							+ "ORDER BY ?start ";

		logger.info("fetchClasses for given question with query {}", sparql);
		final ResultSet classResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		while (classResultSet.hasNext()) {
			QuerySolution s = classResultSet.next();
			Entity entityTemp3 = new Entity(s.getResource("uri").getURI());
			argument.append(entityTemp3.uri + ", ");
			logger.info("uri info {}", s.getResource("uri").getURI());
		}
		return argument;
	}

	private String fetchEntitiesRelationsAndClasses(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		final StringBuilder argument = fetchEntities(qanaryQuestion, qanaryUtils);
		argument.append(fetchRelations(qanaryQuestion,qanaryUtils));
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
		logger.info("Path to SINA JAR file: {}", sinaJarFileLocation);

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
		logger.info("executeExternalSinaJarFile: argument={} on {}", argument, sinaJarFileLocation);

		final ProcessBuilder pb = new ProcessBuilder("java", "-jar", sinaJarFileLocation, argument);
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

		logger.debug("executeExternalSinaJarFile: retrieved output={}", outputRetrieved);

		String queryCandidates = outputRetrieved.substring(outputRetrieved.indexOf("list of final templates:") + "list of final templates:".length());

		logger.info("Found query candidates: {}", queryCandidates);
		
		
		return queryCandidates;
	}

	private String createUpdateQueryFromQueryTemplate(final String[] queryTemplates, final QanaryUtils qanaryUtils, String questionUri) {
		String sparqlPart1 = "";
		String sparqlPart2 = "";
		int x = 10;
		for (int i = 0; i < queryTemplates.length; i++) {
			sparqlPart1 += "" // 
					+ "?a" + i + " a qa:AnnotationOfAnswerSPARQL . " // 
					+ "?a" + i + " oa:hasTarget <"+questionUri+"> . " // 
					+ "?a" + i + " oa:hasBody \"" + queryTemplates[i].replace("\n", " ") + "\" ;" //
					+ "     oa:annotatedBy <urn:qanary:QB#" + SINA.class.getName()+"> ; " //
					+ "         oa:annotatedAt ?time ; " //
					+ "         qa:hasScore " + x-- + " . \n"; 
			sparqlPart2 += "BIND (IRI(str(RAND())) AS ?a" + i + ") . \n";
		}

		final String sparql = "" // 
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " // 
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "INSERT { " //  
							+ "  GRAPH <" + qanaryUtils.getInGraph() + "> { " + sparqlPart1 + "}" // 
							+ "} " //
							+ "WHERE { " //
							+ "  " + sparqlPart2 // 
							+ "  BIND (IRI(str(RAND())) AS ?b) ." //
							+ "  BIND (now() as ?time) . " //
							+ "}";
		return sparql;
	}
	
	private String getQuestionURI(QanaryUtils myQanaryUtils, String namedGraph, String endpoint) throws SparqlQueryFailed {
			String sparql = "" // 
					+ "PREFIX qa:<http://www.wdaqua.eu/qa#> " //
					+ "SELECT ?questionuri " //
					+ "FROM <" + namedGraph + "> " //
					+ "WHERE {?questionuri a qa:Question}";

			ResultSet result = myQanaryUtils.selectFromTripleStore(sparql, endpoint);
			return result.next().getResource("questionuri").toString();
	}
	
	protected class Entity {
		public int begin;
		public int end;
		public String namedEntity;
		public final String uri;
		
		Entity(String uri){
			this.uri = uri;
		}

		Entity(String uri, int begin, int end){
			this.uri = uri;
			this.begin = begin;
			this.end = end;
		}
	}

	/**
	 * computes the absolute SINA JAR file location and checks if the file is present there
	 * 
	 * @param sinaJarFileName
	 * @return
	 * @throws IOException 
	 */
	protected String getValidSinaJarFileAbsoluteLocation(String sinaJarFileName) throws IOException {
		if(sinaJarFileName == null || sinaJarFileName.isEmpty()) {
			throw new NoSinaFileProvidedException();
		}
		
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath*:" + sinaJarFileName);
		String sinaJarFileAbsoluteLocation = "";

	    for(Resource r: resources) {
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
		
		if(!(new File(sinaJarFileAbsoluteLocation).exists())) {
			throw new NoSinaFileNotFoundException(sinaJarFileAbsoluteLocation);
		}
		
		logger.info("Found JAR file ({}) at {}", sinaJarFileName, sinaJarFileAbsoluteLocation);
		return sinaJarFileAbsoluteLocation;
	}
}
