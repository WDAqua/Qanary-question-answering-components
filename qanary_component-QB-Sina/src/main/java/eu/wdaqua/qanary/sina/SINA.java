package eu.wdaqua.qanary.sina;

import java.io.*;
import java.net.URISyntaxException;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

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

	private ResourceLoader resourceLoader;
	private static final Logger logger = LoggerFactory.getLogger(SINA.class);

	public SINA() {
		this.resourceLoader = new DefaultResourceLoader();
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
		QanaryQuestion<String> qanaryQuestion = new QanaryQuestion(myQanaryMessage);
		String myQuestion = qanaryQuestion.getTextualRepresentation();
		logger.info("myQuestion: {}", myQuestion);

		final String argument = fetchEntitiesRelationsAndClasses(qanaryQuestion, qanaryUtils);

		logger.info("Sina Arguments: {}", argument+": "+argument.length());
		logger.info("Sina Argument Count: {}",StringUtils.countMatches(argument, "dbpedia"));
		
		if(argument.length() > 2 && StringUtils.countMatches(argument, "dbpedia") <=3 ) {
			final String[] queryTemplates = runSina(argument);
			final String updateQuery = createUpdateQueryFromQueryTemplate(queryTemplates, qanaryUtils);

			logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
			logger.info("apply vocabulary alignment on outgraph");
			qanaryUtils.updateTripleStore(updateQuery, myQanaryMessage.getEndpoint().toString());
		}
		else {
			logger.info("Argument is Null {}", argument);
		}
		return myQanaryMessage;
	}

	private StringBuilder fetchEntities(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed {
		final String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
							+ "SELECT ?start ?end ?uri " + "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
							+ "		     a               oa:SpecificResource; " //
							+ "		     oa:hasSource    ?q; " //
							+ "	         oa:hasSelector  [ " //
							+ "			         a        oa:TextPositionSelector ; " //
							+ "			         oa:start ?start ; " //
							+ "			         oa:end   ?end " //
							+ "		     ] " //
							+ "    ] . " //
							+ " ?a oa:hasBody ?uri ; "//
							+ "} ";

		logger.info("fetchEntities for given question with query {}", sparql);

		final ResultSet entitiesResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		while (entitiesResultSet.hasNext()) {
			QuerySolution s = entitiesResultSet.next();

			final Entity entity = new Entity();
			entity.begin = s.getLiteral("start").getInt();
			entity.end = s.getLiteral("end").getInt();
			entity.uri = s.getResource("uri").getURI();

			argument.append(entity.uri + ", ");

			logger.info("uri:start:end info {}", entity.uri + entity.begin + entity.end);
		}
		return argument;
	}

	private StringBuilder fetchRelations(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		final String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
							+ "SELECT ?relationurl " + "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "  ?a a qa:AnnotationOfRelation . " + "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
							+ "           oa:hasSource    <" + qanaryQuestion.getUri() + ">; "
							// + " oa:start ?start; " //
							// + " oa:end ?end " //
							+ "  ] ; " + "     oa:hasBody ?relationurl ;"
							// + " oa:annotatedBy <; "
							+ "	    oa:AnnotatedAt ?time  " + "} " //
							+ "ORDER BY ?start ";

		logger.info("fetchRelations for given question with query {}", sparql);

		final ResultSet relationResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		while (relationResultSet.hasNext()) {
			QuerySolution s = relationResultSet.next();

			final Entity entity = new Entity();
			// entityTemp2.begin = s.getLiteral("start").getInt();
			// entityTemp2.end = s.getLiteral("end").getInt();
			entity.uri = s.getResource("relationurl").getURI();

			argument.append(entity.uri + ", ");

			logger.info("uri info {}", entity.uri);
		}
		return argument;
	}

	private StringBuilder fetchClasses(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		final String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
							+ "SELECT ?url " + "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "  ?a a qa:AnnotationOfClass . " + "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
							+ "           oa:hasSource    <" + qanaryQuestion.getUri() + ">; "
							// + " oa:start ?start; " //
							// + " oa:end ?end " //
							+ "  ] ; " + "     oa:hasBody ?url ;"
							// + " oa:annotatedBy <; "
							+ "	    oa:AnnotatedAt ?time  " + "} " //
							+ "ORDER BY ?start ";

		logger.info("fetchClasses for given question with query {}", sparql);
		final ResultSet classResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		while (classResultSet.hasNext()) {
			QuerySolution s = classResultSet.next();
			Entity entityTemp3 = new Entity();
			entityTemp3.uri = s.getResource("uri").getURI();

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

	private String[] runSina(String argument) throws IOException, InterruptedException {
		final String path = new File(System.getProperty("java.class.path")).getParentFile().getAbsolutePath();
		final String sinaJar = path+"/sina-0.0.1.jar";

		logger.info("Path to sina jar file"+sinaJar);

		final ProcessBuilder pb = new ProcessBuilder("java", "-jar", sinaJar, argument);
		final Process p = pb.start();
		p.waitFor();

		String outputRetrived = "";


		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			outputRetrived += line;
		}
		br.close();
		p.destroy();

		logger.debug("The retrived output : " + outputRetrived);
		String queryTemplates = outputRetrived.substring(outputRetrived.indexOf("list of final templates:") + "list of final templates:".length());

		logger.info("Result {}", queryTemplates);
		queryTemplates = queryTemplates.trim();
		queryTemplates = queryTemplates.substring(1, queryTemplates.length() - 1);
		return queryTemplates.split(",");
	}

	private String createUpdateQueryFromQueryTemplate(final String[] queryTemplates, final QanaryUtils qanaryUtils) {
		String sparqlPart1 = "";
		String sparqlPart2 = "";
		int x = 10;
		for (int i = 0; i < queryTemplates.length; i++) {
			sparqlPart1 += "?a" + i + " a qa:AnnotationOfAnswerSPARQL . " + "  ?a" + i + " oa:hasTarget <URIAnswer> . "
					+ "  ?a" + i + " oa:hasBody \"" + queryTemplates[i].replace("\n", " ") + "\" ;"
					+ "     oa:annotatedBy <www.wdaqua.sina> ; " + "         oa:annotatedAt ?time ; "
					+ "         qa:hasScore " + x-- + " . \n";
			sparqlPart2 += "BIND (IRI(str(RAND())) AS ?a" + i + ") . \n";
		}

		final String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> " + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
							+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "INSERT { " + "GRAPH <"
							+ qanaryUtils.getInGraph() + "> { " + sparqlPart1 + "}} " + "WHERE { " + sparqlPart2
							+ "BIND (IRI(str(RAND())) AS ?b) ." + "BIND (now() as ?time) " + "}";
		return sparql;
	}

	class Entity {
		public int begin;
		public int end;
		public String namedEntity;
		public String uri;
	}
}
