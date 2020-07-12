package eu.wdaqua.qanary.languagedetection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * the component detects the languages of the current component
 * 
 * the implementation of this Qanary component is following the typical 3-step
 * process
 *
 */
@Component
public class LanguageDetection extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(LanguageDetection.class);
	private static boolean languageProfileLoaded = false;

	public LanguageDetection() throws IOException, LangDetectException {
		// just do this once as the DetectorFactory will crash otherwise
		if (!languageProfileLoaded) {
			languageProfileLoaded = true;
			// location of the profile directory
			String profileLocation = "language-detection/profiles";
			DetectorFactory.loadProfile(getClass().getClassLoader().getResource(profileLocation).getFile());
		}
	}

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("Qanary Message: {}", myQanaryMessage);

		// STEP1: Retrieve the question
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

		// question string is required as input for the service call
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		// STEP2: The question is send to the language recognition library
		ArrayList<String> languages = (ArrayList<String>) getDetectedLanguages(myQuestion);

		// STEP 3: The language tag is pushed to the Qanary triple store
		myQanaryQuestion.setLanguageText(languages);

		return myQanaryMessage;
	}

	/**
	 * computes the languages for a given textual question
	 * 
	 * The language tags are already aligned with
	 * http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
	 * 
	 * @param myQuestion
	 * @return
	 * @throws LangDetectException
	 */
	public List<String> getDetectedLanguages(String myQuestion) throws LangDetectException {
		Detector detector = DetectorFactory.create();
		detector.append(myQuestion);
		String detectedLangOfGivenQuestion = detector.detect();
		logger.info("for question '{}' the language '{}' was detected.", myQuestion, detectedLangOfGivenQuestion);

		return new ArrayList<>(Arrays.asList(detectedLangOfGivenQuestion));
	}
}
