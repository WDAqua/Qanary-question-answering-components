package eu.wdaqua.qanary.relationlinker;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.inject.Inject;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.relationlinker.RelationLinkerServiceFetcher.Link;

@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties
 * (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
@Deprecated
public class RelationLinker1 extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(RelationLinker1.class);

    private final String applicationName;

	@Inject
	private RelationLinkerConfiguration relationLinkerConfiguration;

	@Inject
	private RelationLinkerServiceFetcher relationLinkerServiceFetcher;

	public RelationLinker1(@Value("${spring.application.name}") final String applicationName) 
			throws Exception
		{
		this.applicationName = applicationName;

		logger.warn("This component is DEPRECATED!\nFunctionality is not guaranteed.");
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
		// TODO: implement processing of question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage,
				myQanaryUtils.getQanaryTripleStoreConnector());
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		// STEP2
		// refactored call to external API 
		logger.info("Question: {}", myQuestion);
		ArrayList<Link> links = relationLinkerServiceFetcher.getLinksForQuestion(
				myQuestion, relationLinkerConfiguration.getEndpoint());

		// STEP3
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)
		for (Link l : links) {
			String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
					+ "prefix dbp: <http://dbpedia.org/property/> "
					+ "INSERT { "
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
					+ "  ?a a qa:AnnotationOfRelation . "
					+ "  ?a oa:hasTarget [ "
					+ "           a    oa:SpecificResource; "
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
					+ "              oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; " //
					+ "              oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  " //
					+ "  ] ; "
					+ "     oa:hasBody <" + l.link + "> ;"
					+ "     oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; "
					+ "	    oa:annotatedAt ?time  "
					+ "}} "
					+ "WHERE { "
					+ "BIND (IRI(str(RAND())) AS ?a) ."
					+ "BIND (now() as ?time) "
					+ "}";
			logger.info("Sparql query {}", sparql);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
		}

		return myQanaryMessage;
	}

}
