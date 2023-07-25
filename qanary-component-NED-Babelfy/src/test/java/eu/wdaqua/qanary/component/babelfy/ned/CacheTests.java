package eu.wdaqua.qanary.component.babelfy.ned;

import eu.wdaqua.qanary.commons.config.CacheConfig;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    void givenRestTemplate_whenRequested_thenLogAndModifyResponse() {

        assertDoesNotThrow(() -> CacheConfig.testCache(
                        myRestTemplate,
                        myCacheOfResponse,
                        testPort,
                        MAX_TIME_SPAN_SECONDS
                )
        );
    }

}
