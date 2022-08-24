package eu.wdaqua.qanary.component.sina.qb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class SinaTest {
    private static final Logger logger = LoggerFactory.getLogger(SinaTest.class);
    @Autowired
    private Environment env;

    @Test
    void testIfSinaFileIsPresent() throws IOException, InterruptedException {
        String sinaFileLocationFromEnv = env.getProperty("sina.jarfilelocation");
        SINA mySINA = new SINA(sinaFileLocationFromEnv);
        String dbpediaLondon = "http://dbpedia.org/resource/London";
        String result = mySINA.executeExternalSinaJarFile(dbpediaLondon);

        logger.debug("testIfSinaFileIsPresent: {}", result);
        assertNotNull("Result of external call should not be null.", result);
        assertTrue(result.length() > 0, //
                "Result of external call should not be empty while providing a valid argument. Here: "
                        + dbpediaLondon + ".");
        assertEquals("[select * where {     <http://dbpedia.org/resource/London>   ?p   ?v0.  }]", result.trim());
    }

}
