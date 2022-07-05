package eu.wdaqua.qanary.sina;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class SinaTest {
	@Autowired
	private Environment env;

	private static final Logger logger = LoggerFactory.getLogger(SinaTest.class);

	@Test
	public void testIfSinaFileIsPresent() throws IOException, InterruptedException {
		String sinaFileLocationFromEnv = env.getProperty("sina.jarfilelocation");
		SINA mySINA = new SINA(sinaFileLocationFromEnv);
		String dbpediaLondon = "http://dbpedia.org/resource/London";
		String result = mySINA.executeExternalSinaJarFile(dbpediaLondon);

		logger.debug("testIfSinaFileIsPresent: {}", result);
		assertNotNull("Result of external call should not be null.", result);
		assertTrue("Result of external call should not be empty while providing a valid argument. Here: "
				+ dbpediaLondon + ".", result.length() > 0);
		assertEquals("[select * where {     <http://dbpedia.org/resource/London>   ?p   ?v0.  }]", result.trim());
	}

}
