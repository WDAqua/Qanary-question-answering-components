package eu.wdaqua.qanary.component.platypuswrapper.qb;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CacheTests {
    // time span for caching, tests wait this time span during the test runs
    protected final static int MAX_TIME_SPAN_SECONDS = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheTests.class);

    private final int testPort;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponse;

    CacheTests(
            @Value(value = "${local.server.port}") int testPort, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponse //
    ) {
        this.testPort = testPort;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponse = myCacheOfResponse;
    }

    @BeforeEach
    public void init() {
        assert this.myRestTemplate != null : "restTemplate cannot be null";
    }

    /**
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    void givenRestTemplate_whenRequested_thenLogAndModifyResponse() throws InterruptedException, URISyntaxException {

        assertNotNull(myRestTemplate);
        assertNotNull(myCacheOfResponse);

        URI testServiceURL0 = new URI("http://localhost:" + testPort + "/");
        URI testServiceURL1 = new URI("http://localhost:" + testPort + "/component-description");

        long initialNumberOfRequests = myCacheOfResponse.getNumberOfExecutedRequests();

        callRestTemplateWithCaching(testServiceURL0, Cache.NOT_CACHED); // cache miss
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        TimeUnit.SECONDS.sleep(MAX_TIME_SPAN_SECONDS + 5); // wait until it is too late for caching
        callRestTemplateWithCaching(testServiceURL0, Cache.NOT_CACHED); // cache miss: too long ago
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(testServiceURL1, Cache.NOT_CACHED); // cache miss: different URI
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(testServiceURL1, Cache.CACHED); // cache hit

        assertEquals(initialNumberOfRequests + 3, myCacheOfResponse.getNumberOfExecutedRequests());

    }

    /**
     * @param uri
     * @param cacheStatus
     * @throws URISyntaxException
     */
    private void callRestTemplateWithCaching(URI uri, Cache cacheStatus) throws URISyntaxException {
        long numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests();
        ResponseEntity<String> responseEntity = myRestTemplate.getForEntity(uri, String.class);
        numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests() - numberOfNewlyExecutedRequests;
        LOGGER.info("numberOfExecutedRequest since last request: new={}, count={}, teststatus={}", //
                numberOfNewlyExecutedRequests, myCacheOfResponse.getNumberOfExecutedRequests(), cacheStatus);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        switch (cacheStatus) {
            case NOT_CACHED:
                assertEquals(1, numberOfNewlyExecutedRequests);
                break;
            case CACHED:
                assertEquals(0, numberOfNewlyExecutedRequests);
                break;
            default:
                fail("Test case misconfigured");
                break;
        }
    }

    private enum Cache {
        CACHED, NOT_CACHED
    }
}
