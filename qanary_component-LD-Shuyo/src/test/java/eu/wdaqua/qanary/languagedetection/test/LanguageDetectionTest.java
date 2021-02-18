package eu.wdaqua.qanary.languagedetection.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.languagedetection.LanguageDetection;

public class LanguageDetectionTest {

	private static LanguageDetection myLanguageDetection;
	private static final Logger logger = LoggerFactory.getLogger(LanguageDetectionTest.class);
	private final short expectedQualityAsPercent = 85;

	@BeforeClass
	public static void initLanguageDetection() throws IOException, LangDetectException {
		myLanguageDetection = new LanguageDetection("LanguageDetectionTest");
	}

	@Test
	public void testUncommonInput() throws LangDetectException, IOException {
		String[] englishQuestions = { //
				"12345", //
				"   ", //
				"---", //
				".", //
				"1" //
		};

		checkDetectedLanguageForQuestion(englishQuestions, null);

	}

	/**
	 * English example questions
	 * 
	 * @throws LangDetectException
	 * @throws IOException
	 */
	@Test
	public void testEnglishLanguages() throws LangDetectException, IOException {
		String[] englishQuestions = { //
				"List all boardgames by GMT.", //
				"Who developed Skype?", //
				"Which people were born in Heraklion?", //
				"In which U.S. state is Area 51 located?", //
				"Who is the mayor of New York City?", //
				"Which countries have places with more than two caves?", //
				"Where did Abraham Lincoln die?", //
				"Which airports does Air China serve?", //
				"Give me all actors starring in movies directed by and starring William Shatner.", //
				"What are the official languages of the Philippines?", //
				"Give me all Danish films.", //
				"Which movies starring Brad Pitt were directed by Guy Ritchie?", //
				"Give me the grandchildren of Bruce Lee.", //
				"Which other weapons did the designer of the Uzi develop?", //
				"Who is the owner of Universal Studios?", //
				"Which state of the USA has the highest population density?", //
				"Which monarchs were married to a German?", //
				"Which organizations were founded in 1950?", //
				"Who created the comic Captain America?", //
				"Give me the Apollo 14 astronauts.", //
				"Who wrote the book The pillars of the Earth?", //
				"Which state of the United States of America has the highest density?", //
				"Which spaceflights were launched from Baikonur?", //
				"Give me a list of all trumpet players that were bandleaders.", //
				"Which U.S. states are in the same timezone as Utah?", //
				"Which U.S. states possess gold minerals?", //
				"Who is the daughter of Ingrid Bergman married to?", //
				"How deep is Lake Placid?", //
				"Show me all museums in London.", //
				"Which caves have more than 3 entrances?" //
		};

		checkDetectedLanguageForQuestion(englishQuestions, "en");
	}

	/**
	 * English example questions
	 * 
	 * @throws LangDetectException
	 * @throws IOException
	 */
	@Test
	public void testGermanLanguages() throws LangDetectException, IOException {
		String[] germanQuestions = { //
				"Liste alle Brettspiele von GMT. ", //
				"Wer entwickelt Skype? ", //
				"Welche Menschen wurden geboren im Heraklion? ", //
				"Im welche US Zustand ist Area 51 gelegen? ", //
				"Wer ist der Bürgermeister von New York City?", //
				"In welchen Ländern gibt es Orte mit mehr als zwei Höhlen?", //
				"Wo ist Abraham Lincoln gestorben? ", //
				"Welche Flughäfen fliegt Air China an?", //
				"Gib mir alle Schauspieler von Filmen, in denen William Shatner sowohl Regie geführt als auch selber mitgespielt hat.", //
				"Was sind die Amtssprachen der Philippinen?", //
				"Gib mir alle dänischen Filme.", //
				"Bei welchen Filmen, in denen Brad Pitt mitspielt, hat Guy Ritchie Regie geführt?", //
				"Gib mir die Enkel von Bruce Lee.", //
				"Welche anderen Waffen hat der Erfinder der Uzi entwickelt?", //
				"Wem gehören die Universal Studios?", //
				"Welcher Staat in den USA hat die höchste Bevölkerungsdichte?", //
				"Welche Monarchen waren mit jemand deutschem verheiratet?", //
				"Welche Unternehmen wurden 1950 gegründet?", //
				"Wer hat den Comic Captain America erfunden?", //
				"Gib mir alle Apollo-14-Astronauten.", //
				"Wer schrieb das Buch Das Säulen von das Erde? ", //
				"Welcher Staat der Vereinigten Staaten von Amerika hat die höchste Dichte? ", //
				"Welche Raumflüge sind von Baikonur gestartet?", //
				"Gib mit eine Liste aller Trompeter, die Bandleader waren.", //
				"Welche US-Bundesstaaten liegen in derselben Zeitzone wie Utah?", //
				"In welchen US-Staaten gibt es Goldvorkommen?", //
				"Mit wem ist die Tochter von Ingrid Bergman verheiratet?", //
				"Wie tief ist Lake Placid?", //
				"Zeig mir alle Museen in London.", //
				"Welche Höhlen haben mehr als 3 Eingänge?" //
		};

		checkDetectedLanguageForQuestion(germanQuestions, "de");
	}

	/**
	 * checks an array of questions
	 * 
	 * @param questions
	 * @param expectedLanguage
	 * @throws LangDetectException
	 */
	private void checkDetectedLanguageForQuestion(String[] questions, String expectedLanguage)
			throws LangDetectException {

		int countCorrectLanguageDetections = 0;
		Boolean result;
		
		for (int i = 0; i < questions.length; i++) {
			result = checkDetectedLanguageForQuestion(questions[i], expectedLanguage);
			
			// for null we just check if nothing was crashing
			if (expectedLanguage == null || result == true ) {
				countCorrectLanguageDetections++;
			}
		}

		double ratio = countCorrectLanguageDetections / (double) questions.length * 100;
		logger.info("Language {}: {} of {} were detected correctly (ratio: {}%).", //
				expectedLanguage, countCorrectLanguageDetections, questions.length, ratio);
		assertTrue("Language detection quality not sufficient (" + ratio + "%). " //
				+ "From " + questions.length + " (" + expectedLanguage + ") questions only "
				+ countCorrectLanguageDetections + " were detected correctly.", //
				ratio >= expectedQualityAsPercent);
	}

	/**
	 * checks a particular question
	 * 
	 * @param textualQuestion
	 * @param expectedLanguage
	 * @return
	 * @throws LangDetectException
	 */
	private Boolean checkDetectedLanguageForQuestion(String textualQuestion, String expectedLanguage)
			throws LangDetectException {

		ArrayList<String> detectedLanguages;
		detectedLanguages = (ArrayList<String>) myLanguageDetection.getDetectedLanguages(textualQuestion);
		String detectedLanguage = detectedLanguages.get(0);
		String logmessage;

		logger.info("checkDetectedLanguageForQuestion: 0. {}", detectedLanguage);

		// should be just one detected language, however, we never know
		for (int i = 0; i < detectedLanguages.size(); i++) {
			logmessage = String.format("For '%s' was detected (%d. of %d): %s (expected: %s)", //
					textualQuestion, i, detectedLanguages.size(), detectedLanguages.get(i), expectedLanguage);
			if (expectedLanguage == null) {
				logger.info("IGNORE (null expected): {}", logmessage);
			} else if (expectedLanguage.equalsIgnoreCase(detectedLanguage)) {
				logger.debug(logmessage);
			} else {
				logger.warn(logmessage);
			}
		}

		if (detectedLanguages.size() == 0) {
			logger.warn("no language detected for {}", textualQuestion);
		} else if (detectedLanguages.size() > 1) {
			logger.warn("many ({}) languages detected for {}", detectedLanguages.size(), textualQuestion);
		} else {
			// standard case
		}

		if (detectedLanguage == null) {
			return null; // 
		} else {
			return expectedLanguage.equalsIgnoreCase(detectedLanguage);
		}
	}

}
