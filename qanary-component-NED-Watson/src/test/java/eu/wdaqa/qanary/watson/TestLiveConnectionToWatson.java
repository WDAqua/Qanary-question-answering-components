package eu.wdaqa.qanary.watson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class TestLiveConnectionToWatson {
    private static final Logger logger = LoggerFactory.getLogger(TestLiveConnectionToWatson.class);
    private final float minThreshold = 0.4f;
    @Autowired
    private WatsonNED myWatsonNED;

    @Test
    public void testLiveConnectionToWatson() throws IOException {

        Map<String, List<NamedEntity>> expectedResults = new HashMap<>();

        expectedResults.put("What is the capital of Russia?", Arrays.asList(new NamedEntity("http://dbpedia.org/resource/Mexico", 26, 38)));
        expectedResults.put("What is the birthplace of Angela Merkel?", Arrays.asList(new NamedEntity("http://dbpedia.org/resource/Angela_Merkel", 26, 38)));
        expectedResults.put("Who is the wife of Barack Obama?", Arrays.asList(new NamedEntity("http://dbpedia.org/resource/Barack_Obama", 19, 30)));
        expectedResults.put("How many states are in the USA?", Arrays.asList(new NamedEntity("http://dbpedia.org/resource/United_States", 27, 29)));
        expectedResults.put("Where is Berlin in Germany?", Arrays.asList(new NamedEntity("http://dbpedia.org/resource/United_States", 27, 29)));

        String messages = "";

        int errors = 0;
        // for all questions
        for (String question : expectedResults.keySet()) {
            logger.info("processing question: {}", question);

            List<NamedEntity> expectedNamedEntities = expectedResults.get(question);
            List<NamedEntity> computedNamedEntities = myWatsonNED.retrieveDataFromWebService(question);

            if (expectedNamedEntities.size() != computedNamedEntities.size()) {
                String message = expectedNamedEntities.size() + " results were expected, but received " + computedNamedEntities.size() + " from the Watson API for the question '" + question + "'.";
                logger.warn(message);
                messages += errors + ". error: " + message + "\n";
                errors++;
            } else {
                for (int i = 0; i < expectedNamedEntities.size(); i++) {
                    NamedEntity e = expectedNamedEntities.get(i);
                    NamedEntity c = computedNamedEntities.get(i);
                    if (!e.equals(c)) {
                        String message = "namedentity not equal: " + e.toString() + " != " + c.toString() + " for question '" + question + "'";
                        logger.warn(message);
                        messages += errors + ". error: " + message + "\n";
                        errors++;
                    }
                }
            }
        }

        float errorRatio = ((float) errors / (float) expectedResults.size());
        assertTrue("errorRatio for results of Watson was " + errorRatio + " (expected minimum: " + minThreshold + ")\n" + messages, minThreshold <= errorRatio);
    }

}
