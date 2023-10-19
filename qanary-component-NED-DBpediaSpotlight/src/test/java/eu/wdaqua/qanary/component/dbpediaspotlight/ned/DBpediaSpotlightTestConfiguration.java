package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.springframework.boot.test.context.TestConfiguration;

import java.io.IOException;

@TestConfiguration
public class DBpediaSpotlightTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("qanary.webservicecalls.cache.specs", "maximumSize=1000,expireAfterAccess=" + CacheTests.MAX_TIME_SPAN_SECONDS + "s");
        // deactivate the live test of the real-world webservice
        System.setProperty("dbpediaspotlight.perform-live-check-on-component-start", "false");
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
