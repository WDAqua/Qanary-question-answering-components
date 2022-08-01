package eu.wdaqua.qanary.mypackage;

import java.util.ArrayList;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.mypackage.BabelfyServiceFetcher.Link;

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
public class BabelfyNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BabelfyNED.class);

	private final String applicationName;

	@Inject
	private BabelfyServiceFetcher babelfyServiceFetcher;

	@Inject
	private BabelfyConfiguration babelfyConfiguration;

	public BabelfyNED(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
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
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		logger.info("Question {}", myQuestion);
		ArrayList<Link> links = babelfyServiceFetcher.getLinksForQuestion(
				babelfyConfiguration.getEndpoint(), myQuestion, babelfyConfiguration.getParameters()
				);


		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		for (Link l : links) {
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfInstance . " //
					+ "  ?a oa:hasTarget [ " //
					+ "           a    oa:SpecificResource; " //
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
					+ "           oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  " //
					+ "           ] " //
					+ "  ] . " //
					+ "  ?a oa:hasBody <" + l.link + "> ;" //
					+ "     oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; " //
					+ "	    oa:annotatedAt ?time  " + "}} " //
					+ "WHERE { " //
					+ "  BIND (IRI(str(RAND())) AS ?a) ."//
					+ "  BIND (now() as ?time) " //
					+ "}";
			logger.debug("Sparql query: {}", sparql);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
		}
		return myQanaryMessage;
	}
}
