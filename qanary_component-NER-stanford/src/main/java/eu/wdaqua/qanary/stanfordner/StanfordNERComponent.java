package eu.wdaqua.qanary.stanfordner;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter for English questions
 *
 * @author Dennis Diefenbach
 */

@Component
public class StanfordNERComponent extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(StanfordNERComponent.class);
	private StanfordCoreNLP myStanfordCoreNLP;

	private final String applicationName;

	public StanfordNERComponent(@Value("${spring.application.name}") final String applicationName) {
		// ATTENTION: This should be done only ones when the component is started
		// Define the properties needed for the pipeline of the Stanford parser
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		props.setProperty("ner.useSUTime", "false");
		myStanfordCoreNLP = new StanfordCoreNLP(props);

		this.applicationName = applicationName;
	}

	/**
	 * default processor of a QanaryMessage
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		long startTime = System.currentTimeMillis();

		logger.info("Qanary Message: {}", myQanaryMessage);

		// STEP1: Retrieve the named graph and the Qanary triplestore endpoint
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		// STEP2: Pass the information to the component and execute it
		ArrayList<Selection> selections = annotateQuestion(myQuestion);

		// STEP3: Push the result of the component to the Qanary triplestore
		logger.info("Apply commons alignment on outgraph");
		for (Selection s : selections) {
			insertSelectionsIntoQanaryTriplestore(s, myQanaryQuestion, myQanaryUtils);
		}
		long estimatedTime = System.currentTimeMillis() - startTime;
		logger.info("Time: {}", estimatedTime);

		return myQanaryMessage;
	}

	protected ArrayList<Selection> annotateQuestion(String myQuestion) {
		// Create an empty annotation just with the given text
		Annotation document = new Annotation(myQuestion);
		// Run the Stanford annotator on question
		myStanfordCoreNLP.annotate(document);
		// Identify which parts of the question is tagged by the NER tool
		ArrayList<Selection> selections = new ArrayList<>();

		// stores the last token with non-zero
		// tag, if it does not exist set to null
		CoreLabel startToken = null;
		// stores the last found token with non-zero tag,
		// if it does not exist, then set to null
		// Note that consequent non-zero tokens with the same tag like " 0 PERSON PERSON
		// 0 " must be considered together
		// Iterate over the tags
		CoreLabel endToken = null;

		for (CoreLabel token : document.get(TokensAnnotation.class)) {
			logger.info("Tagged question (token ---- tag): {}",
					token.toString() + "  ----  " + token.get(NamedEntityTagAnnotation.class));
			if (!token.get(NamedEntityTagAnnotation.class).equals("O")) {
				if (startToken == null) {
					startToken = token;
					endToken = token;
				} else {
					if (startToken.get(NamedEntityTagAnnotation.class) == token.get(NamedEntityTagAnnotation.class)) {
						endToken = token;
					} else {
						Selection s = new Selection(startToken.beginPosition(), endToken.endPosition(), myQuestion);
						selections.add(s);
						startToken = token;
						endToken = token;
					}
				}
			} else if (startToken != null) {
				Selection s = new Selection(startToken.beginPosition(), endToken.endPosition(), myQuestion);
				selections.add(s);
				startToken = null;
				endToken = null;
			}
		}
		if (startToken != null) {
			Selection s = new Selection(startToken.beginPosition(), endToken.endPosition(), myQuestion);
			selections.add(s);
		}
		return selections;
	}

	protected void insertSelectionsIntoQanaryTriplestore(Selection s, QanaryQuestion<String> myQanaryQuestion,
			QanaryUtils myQanaryUtils)
			throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " //
				+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
				+ "  ?a a qa:AnnotationOfSpotInstance . " //
				+ "  ?a oa:hasTarget [ " //
				+ "           a    oa:SpecificResource; " //
				+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
				+ "           oa:hasSelector  [ " //
				+ "                    a oa:TextPositionSelector ; " //
				+ "                    oa:start \"" + s.begin + "\"^^xsd:nonNegativeInteger ; " //
				+ "                    oa:end  \"" + s.end + "\"^^xsd:nonNegativeInteger  " //
				+ "           ] " //
				+ "  ] ; " //
				+ "     oa:annotatedBy <urn:qanary:"+this.applicationName+"> ;" //
				+ "	    oa:annotatedAt ?time  " //
				+ "}} " //
				+ "WHERE { " //
				+ "  BIND (IRI(str(RAND())) AS ?a) ." //
				+ "  BIND (now() as ?time) " //
				+ "}";
		myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
	}

	protected class Selection {
		private final int begin;
		private final int end;
		private final String question;

		private Selection(int begin, int end, String question) {
			this.begin = begin;
			this.end = end;
			this.question = question;
		}

		protected int getBegin() {
			return this.begin;
		}

		protected int getEnd() {
			return this.end;
		}

		protected String getIdentifiedEntity() {
			return question.substring(begin, end);
		}
	}

}
