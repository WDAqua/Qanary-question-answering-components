package eu.wdaqua.qanary.component.stanford.ner;

import eu.wdaqua.qanary.component.stanford.ner.StanfordNERComponent.Selection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertTrue;


class StanfordNERComponentTest {

    private static final Logger logger = LoggerFactory.getLogger(StanfordNERComponentTest.class);
    private static StanfordNERComponent myStanfordNERComponent;
    private final double expectedMinimumRatioOfCorrectAnnotations = 90.0d;

    @BeforeAll
    public static void initStanfordNERComponent() {
        myStanfordNERComponent = new StanfordNERComponent("stanfordNER");
    }

    public void extendQuestionsMap(HashMap<String, ArrayList<String>> questions, String myQuestion,
                                   String[] annotations) {
        ArrayList<String> a = new ArrayList<>();
        for (int i = 0; i < annotations.length; i++) {
            a.add(annotations[i]);
        }
        questions.put(myQuestion, a);
    }

    @Test
    void testAnnotationQuality() {

        HashMap<String, ArrayList<String>> questionsAndAnnotations = new HashMap<String, ArrayList<String>>();

        String[] annotations = {"Air China"};
        extendQuestionsMap(questionsAndAnnotations, "Which airports does Air China serve?", annotations);

        String[] annotations2 = {"Skype"};
        extendQuestionsMap(questionsAndAnnotations, "Who developed Skype?", annotations2);

        String[] annotations3 = {"Heraklion"};
        extendQuestionsMap(questionsAndAnnotations, "Which people were born in Heraklion?", annotations3);

        String[] annotations4 = {"New York City", "mayor"};
        extendQuestionsMap(questionsAndAnnotations, "Who is the mayor of New York City?", annotations4);

        String[] annotations5 = {"Abraham Lincoln"};
        extendQuestionsMap(questionsAndAnnotations, "Where did Abraham Lincoln die?", annotations5);

        String[] annotations6 = {"Philippines"};
        extendQuestionsMap(questionsAndAnnotations, "What are the official languages of the Philippines?",
                annotations6);

        String[] annotations7 = {"Brad Pitt", "Guy Ritchie"};
        extendQuestionsMap(questionsAndAnnotations, "Which movies starring Brad Pitt were directed by Guy Ritchie?",
                annotations7);

        String[] annotations8 = {"Danish"};
        extendQuestionsMap(questionsAndAnnotations, "Give me all Danish films", annotations8);

        String[] annotations9 = {"Bruce Lee"};
        extendQuestionsMap(questionsAndAnnotations, "Give me the grandchildren of Bruce Lee.", annotations9);

        String[] annotations10 = {"two"};
        extendQuestionsMap(questionsAndAnnotations, "\"Which countries have places with more than two caves?",
                annotations10);

        int countCorrectAnnotations = 0;
        for (Entry<String, ArrayList<String>> entry : questionsAndAnnotations.entrySet()) {
            String question = entry.getKey();
            ArrayList<String> expectedAnnotations = entry.getValue();
            if (checkExpectedAnnotations(question, expectedAnnotations)) {
                countCorrectAnnotations++;
            }
        }

        double ratio = countCorrectAnnotations / (double) questionsAndAnnotations.size() * 100;
        assertTrue(ratio >= expectedMinimumRatioOfCorrectAnnotations,
                String.format("Ratio of %f  not good enough. Expected at least %f .", //
                        ratio, expectedMinimumRatioOfCorrectAnnotations));

    }

    private boolean checkExpectedAnnotations(String question, ArrayList<String> expectedAnnotations) {
        ArrayList<Selection> selections = myStanfordNERComponent.annotateQuestion(question);
        ArrayList<String> computedAnnotations = new ArrayList<>();
        for (Selection selection : selections) {
            computedAnnotations.add(selection.getIdentifiedEntity());
        }

        Collections.sort(expectedAnnotations);
        Collections.sort(computedAnnotations);

        if (!expectedAnnotations.equals(computedAnnotations)) {
            logger.warn("Annotations not equal. Expected: {}, Computed: {}", expectedAnnotations, computedAnnotations);
        }

        return expectedAnnotations.equals(computedAnnotations);
    }
}
