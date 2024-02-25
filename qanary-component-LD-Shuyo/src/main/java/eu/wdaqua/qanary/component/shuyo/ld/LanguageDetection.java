package eu.wdaqua.qanary.component.shuyo.ld;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
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
	private final String FILENAME_ANNOTATIONS_FILTERED = "/queries/insert_one_AnnotationOfQuestionLanguage.rq";

	private static final Logger logger = LoggerFactory.getLogger(LanguageDetection.class);
	private static boolean languageProfileLoaded = false;
	private final String applicationName;

	public LanguageDetection(@Value("${spring.application.name}") final String applicationName)
			throws IOException, LangDetectException {

		this.applicationName = applicationName;

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_ANNOTATIONS_FILTERED);

		this.safeLoadProfile();
	}

	/**
	 * load profile with several fallbacks
	 *
	 * @throws IOException
	 * @throws LangDetectException
	 */
	private void safeLoadProfile() throws IOException, LangDetectException {
		// just do this once as the DetectorFactory will crash otherwise
		if (!languageProfileLoaded) {
			languageProfileLoaded = true;
			// location of the profile directory
			String profileLocation = "language-detection/profiles/";

			// main problem in LangDetect exists while referring to a profile directory
			// inside of a JAR file
			try {
				// running application OUTSIDE of JAR files
				DetectorFactory.loadProfile(getClass().getClassLoader().getResource(profileLocation).getFile());
			} catch (Exception e) {
				// running application INSIDE of JAR files
				String dirname = profileLocation;
				Enumeration<URL> en = Detector.class.getClassLoader().getResources(dirname);
				List<String> profiles = new ArrayList<>();
				if (en.hasMoreElements()) {
					URL url = en.nextElement();
					JarURLConnection urlcon = (JarURLConnection) url.openConnection();
					try (JarFile jar = urlcon.getJarFile();) {
						Enumeration<JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							String entry = entries.nextElement().getName();
							if (entry.startsWith(dirname)) {
								try (InputStream in = Detector.class.getClassLoader().getResourceAsStream(entry);) {
									profiles.add(IOUtils.toString(in));
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}
						}
					}
				}

				DetectorFactory.loadProfile(profiles);
			}
		}
	}

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("Qanary Message: {}", myQanaryMessage);

		// STEP 1: Retrieve the question
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion();

		// question string is required as input for the service call
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		// STEP 2: The question is send to the language recognition library
		ArrayList<String> languages = (ArrayList<String>) getDetectedLanguages(myQuestion);

		// STEP 3: The language tag is pushed to the Qanary triple store
		this.setLanguageText(languages, myQanaryQuestion, this.getUtils());

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
		String detectedLangOfGivenQuestion = null;
		try {
			detectedLangOfGivenQuestion = detector.detect();
		} catch (LangDetectException lde) {
			logger.warn(lde.getMessage());
			detectedLangOfGivenQuestion = null;
		}

		logger.info("for question '{}' the language '{}' was detected.", myQuestion, detectedLangOfGivenQuestion);

		return new ArrayList<>(Arrays.asList(detectedLangOfGivenQuestion));
	}

	public void setLanguageText(List<String> languages, QanaryQuestion<?> myQanaryQuestion, QanaryUtils myQanaryUtils) throws Exception {
		for (int i = 0; i < languages.size(); i++) {
			QuerySolutionMap bindings = new QuerySolutionMap();
			// use here the variable names defined in method insertAnnotationOfAnswerSPARQL
			bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
			bindings.add("hasTarget", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
			bindings.add("hasBody", ResourceFactory.createStringLiteral(languages.get(i)));
			bindings.add("annotatedBy", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

			// get the template of the INSERT query
			String sparql = this.loadQueryFromFile(FILENAME_ANNOTATIONS_FILTERED, bindings);
			logger.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);

			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);
		}
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}

}
