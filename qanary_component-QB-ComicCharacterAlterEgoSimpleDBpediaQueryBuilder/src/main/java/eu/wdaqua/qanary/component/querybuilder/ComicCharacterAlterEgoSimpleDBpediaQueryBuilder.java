package eu.wdaqua.qanary.component.querybuilder;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

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

	public ComicCharacterAlterEgoSimpleDBpediaQueryBuilder(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}
	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception 
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		//read question from database
		QanaryQuestion<String> qanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
		String question = qanaryQuestion.getTextualRepresentation();
		QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);

		String prefix = "what is the real name of";
		
		//return if question does not start with supported prefix
		if(!question.toLowerCase().startsWith(prefix)) {
			logger.info("Question \"{}\" does not start with \"{}\" - aborting process...",question,prefix);
			return myQanaryMessage;
		}

		String getAnnotation = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "SELECT ?a ?dbpediaResource ?startOfSpecificResource ?endOfSpecificResource ?annotatorComponent ?time " //
				+ "FROM <" + myQanaryMessage.getInGraph().toString() + "> " //
				+ "WHERE {" //
				+ "   VALUES ?dbpediaResource {" //
				+ "      <" + qanaryQuestion.getUri().toString() + ">" //
				+ "   } ." //
				+ "   ?a a qa:AnnotationOfSpotInstance ." //
				+ "   ?a oa:hasTarget [" //
				+ "                    a               oa:SpecificResource;" //
				+ "                    oa:hasSource    ?dbpediaResource;" //
				+ "                    oa:hasSelector  [ " //
				+ "                                     a        oa:TextPositionSelector ; " //
				+ "                                     oa:start ?startOfSpecificResource ; " //
				+ "                                     oa:end   ?endOfSpecificResource " //
				+ "                                    ]" //
				+ "                  ] ." //
				+ "    ?a oa:annotatedAt ?time ." //
// 					   The component was originally built to look specifically for
// 					   annotations of ComicCharacterNameSimpleNamedEntityRecognizer:
//				+ "    ?a oa:annotatedBy <urn:qanary:component:ComicCharacterNameSimpleNamedEntityRecognizer> ." //
				+ "}";


		ResultSet resultSet = qanaryUtils.selectFromTripleStore(getAnnotation);

		if(!resultSet.hasNext()){
			logger.warn("no matching resource could be found!");
		}

		while(resultSet.hasNext()){
			QuerySolution result = resultSet.next();
			logger.info("result: \n{}", result);
			int start = result.get("startOfSpecificResource").asLiteral().getInt();
			int end = result.get("endOfSpecificResource").asLiteral().getInt();
			String name = question.substring(start,end);
			logger.warn("annotation found for name '{}' (at {},{})",name,start,end);

			String dbpediaQuery = "" //
					+ "PREFIX dbr: <http://dbpedia.org/resource/>\n" //
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" //
					+ "PREFIX dbp: <http://dbpedia.org/property/>\n" //
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" //
					+ "PREFIX dct: <http://purl.org/dc/terms/>\n" //
					+ "SELECT * WHERE {\n" //
					+ "  ?resource dbp:alterEgo ?answer .\n" //
					+ "  ?resource rdfs:label ?label .\n" //
					+ "  ?resource dct:subject dbr:Category:Superheroes_with_alter_egos .\n" // only superheros
					+ "  FILTER(LANG(?label) = \"en\") .\n" //
					+ "  FILTER(! strStarts(LCASE(?label), LCASE(?answer))).\n" // filter starting with the same name
					+ "  FILTER(CONTAINS(STR(?label), '"+name+"')) .\n" //
					+ "} \n";

			String insertQuery = "" //
					+ "PREFIX dbr: <http://dbpedia.org/resource/>" //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" //
					+ "" //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryMessage.getInGraph().toString() + ">  {" //
					+ "        ?newAnnotation rdf:type qa:AnnotationOfAnswerSPARQL ." //
					+ "        ?newAnnotation oa:hasTarget [ " //
					+ "                 a    oa:SpecificResource; " //
					+ "                 oa:hasSource    <" + qanaryQuestion.getUri() + ">; " //
					+ "        ] . " //
					+ "        ?newAnnotation oa:hasBody \""
					+ dbpediaQuery.replace("\"", "\\\"").replace("\n", "\\n") + "\"^^xsd:string ." //
					+ "        ?newAnnotation qa:score \"1.0\"^^xsd:float ."
					+ "        ?newAnnotation oa:annotatedAt ?time ." //
					+ "        ?newAnnotation oa:annotatedBy <urn:qanary:component:"+this.applicationName+"> ." //
					+ "    }" //
					+ "}" //
					+ "WHERE {" //
					+ "    BIND (IRI(str(RAND())) AS ?newAnnotation) ." //
					+ "    BIND (now() as ?time) . " //
					+ "}";

			logger.info("The answer might be computed via: \n{}", dbpediaQuery);
			qanaryUtils.updateTripleStore(insertQuery, myQanaryMessage.getEndpoint());
		}
		return myQanaryMessage;
	}

}
