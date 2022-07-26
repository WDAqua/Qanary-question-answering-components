package eu.wdaqua.qanary.rubq.wrapper;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.springframework.boot.test.context.TestConfiguration;

import java.io.IOException;

@TestConfiguration
public class RuBQTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("rubq.endpoint.url", "http://some-rubq-endpoint-url.com/endpoint");
        System.setProperty("rubq.mock.endpoint.url", "http://some-rubq-endpoint-url.com/endpoint");
        System.setProperty("qanary.webservicecalls.cache.specs", "maximumSize=1000,expireAfterAccess=" + RuBQQueryBuilderTest.MAX_TIME_SPAN_SECONDS + "s");
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