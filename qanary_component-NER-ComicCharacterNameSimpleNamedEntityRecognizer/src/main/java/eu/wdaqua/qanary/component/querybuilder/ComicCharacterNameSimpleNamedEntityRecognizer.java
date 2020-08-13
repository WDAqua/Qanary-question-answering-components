package eu.wdaqua.qanary.component.querybuilder;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import java.net.URISyntaxException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

	public ComicCharacterNameSimpleNamedEntityRecognizer(@Value("${spring.application.name}") final String applicationName){
		this.applicationName = applicationName;
	}

	/**
	 * try to find a superhero name in the given question using a trivial string
	 * matching for entity recognition the label of the entities are fetched from
	 * DBpedia every time
	 * 
	 * @throws Exception
	 */
	private SuperheroNamedEntityFound getAllSuperheroNamesFromDBpediaMatchingPositions(String question) {

		// query DBpedia for all superhero film characters
		String serviceUrl = "http://dbpedia.org/sparql";
		String query = "" //
				+ "PREFIX dbr: <http://dbpedia.org/resource/>\n" //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" //
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" //
				+ "PREFIX pt: <http://purl.org/dc/terms/>\n" //
				+ "SELECT ?hero (str(?herolabel) as ?herolabelString) (lang(?herolabel) as ?herolabelLang)\n" //
				+ "WHERE {\n" //
				+ "  ?hero pt:subject dbr:Category:Superhero_film_characters .\n" //
				+ "  ?hero rdfs:label ?herolabel.\n" //
				+ "}\n" //
				+ "LIMIT 10000";

		logger.info("searching for character names on DBpedia ...\nDBpedia query: \n{}", query);

		QueryExecution qe = QueryExecutionFactory.sparqlService(serviceUrl, query);
		ResultSet rs = qe.execSelect();
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
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.info("process: {}", myQanaryMessage);

		// fetching question from database
		QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> qanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String triplestore = qanaryQuestion.getEndpoint().toString();

		String question = null;
		try {
			question = qanaryQuestion.getTextualRepresentation();
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
			String sparqlInsert = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" //
					+ "INSERT { \n" //
					+ "GRAPH <" + qanaryQuestion.getOutGraph() + "> { \n" //
					+ "  ?a a qa:AnnotationOfSpotInstance . \n" //
					+ "  ?a oa:hasTarget [ \n" //
					+ "       a    oa:SpecificResource; \n" //
					+ "       oa:hasSource    <" + qanaryQuestion.getUri() + ">; \n" //
					+ "       oa:hasSelector  [ \n" //
					+ "          a oa:TextPositionSelector ; " //
					+ "          oa:start \"" + foundSuperhero.getBeginIndex() + "\"^^xsd:nonNegativeInteger ; \n" //
					+ "          oa:end  \"" + foundSuperhero.getEndIndex() + "\"^^xsd:nonNegativeInteger  \n" //
					+ "       ] \n" //
					+ "     ] ; \n" //
					+ "     oa:annotatedBy <urn:qanary:component:"+this.applicationName+"> ; \n" //
					+ "	    oa:annotatedAt ?time  \n" //
					+ "}} \n" //
					+ "WHERE { \n" //
					+ "  BIND (IRI(str(RAND())) AS ?a) .\n" //
					+ "  BIND (now() as ?time) \n" //
					+ "}"; //

			qanaryUtils.updateTripleStore(sparqlInsert, triplestore);

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
		}

		logger.info("Component processing finished successfully. Annotation of {} inserted into Qanary triplestore {}.",
				foundSuperhero.getSuperheroLabel(), triplestore);
		return myQanaryMessage;
	}
}
