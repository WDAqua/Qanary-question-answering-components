package eu.wdaqua.qanary.component.meaningcloud.ner;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@org.springframework.boot.test.context.TestConfiguration
public class MeaningCloudTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("question1", "What is the birthplace of Albert Einstein?");
        System.setProperty("question2", "What is the capital of Germany?");
        System.setProperty("question3", "What is the real name of Batman?");

        System.setProperty("qanary.webservicecalls.cache.specs", "maximumSize=1000,expireAfterAccess=" + CacheTests.MAX_TIME_SPAN_SECONDS + "s");
    }

    /**
     * get the content from the defined file
     *
     * @return
     * @throws IOException
     */
    protected static String getStringFromFile(String filename) throws IOException {
        String path = MeaningCloudTestConfiguration.class.getClassLoader().getResource(filename).getPath();

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