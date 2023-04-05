package eu.wdaqua.qanary.component.agdistis.ned;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.springframework.boot.test.context.TestConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@TestConfiguration
public class AgdistisTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("question1", "What is the birthplace of Albert Einstein?");
        System.setProperty("question2", "What is the capital of Germany?");
        System.setProperty("question3", "What is the real name of Batman?");

        System.setProperty("qanary.webservicecalls.cache.specs", "maximumSize=1000,expireAfterAccess=" + AgdistisCacheTests.MAX_TIME_SPAN_SECONDS + "s");
    }

    /**
     * get the content from the defined file
     *
     * @return
     * @throws IOException
     */
    protected static String getStringFromFile(String filename) throws IOException {
        String path = AgdistisTestConfiguration.class.getClassLoader().getResource(filename).getPath();

        return new String(Files.readAllBytes(Paths.get(path)));
    }

    /**
     * get the defined SPARQL query and remove all control characters (like newline)
     *
     * @return
     * @throws IOException
     */
    protected static String getTestQuery(String testQueryFilename) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResources(testQueryFilename).replaceAll("\\p{Cntrl}", "");
    }

}