package eu.wdaqua.qanary.languagedetection;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
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
	private final String applicationName;

	public LanguageDetection(@Value("${spring.application.name}") final String applicationName)
			throws IOException, LangDetectException {

		this.applicationName = applicationName;

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
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);

		// STEP 1: Retrieve the question
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

		// question string is required as input for the service call
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		// STEP 2: The question is send to the language recognition library
		ArrayList<String> languages = (ArrayList<String>) getDetectedLanguages(myQuestion);

		// STEP 3: The language tag is pushed to the Qanary triple store
		this.setLanguageText(languages, myQanaryQuestion.getUri(), myQanaryMessage.getOutGraph(),
				myQanaryMessage.getEndpoint(), myQanaryUtils);

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

	public void setLanguageText(List<String> languages, URI question, URI outGraph, URI endpoint,
			QanaryUtils myQanaryUtils) throws Exception {
		String part = "";
		for (int i = 0; i < languages.size(); i++) {
			part += "?a oa:hasBody \"" + languages.get(i) + "\" . ";
		}
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "INSERT { " //
				+ "	GRAPH <" + outGraph + "> { " //
				+ "		?a a qa:AnnotationOfQuestionLanguage . " //
				+ part //
				+ "		?a 	oa:hasTarget <" + question + "> ; " //
				+ "   		oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; " //
				+ "   		oa:annotatedAt ?time  " //
				+ " } " //
				+ "} " //
				+ "WHERE { " //
				+ "	BIND (IRI(str(RAND())) AS ?a) . " //
				+ "	BIND (now() as ?time) . " //
				+ "}";
		myQanaryUtils.updateTripleStore(sparql, endpoint);
	}

}
