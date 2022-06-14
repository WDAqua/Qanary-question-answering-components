package eu.wdaqua.qanary.rubq_wrapper;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class RestTemplateCacheLiveTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("rubq.endpoint.url", "http://some-rubq-endpoint-url.com/endpoint");
        System.setProperty("qanary.webservicecalls.cache.specs",
                "maximumSize=1000,expireAfterAccess=" + TestRuBQQueryBuilder.MAX_TIME_SPAN_SECONDS + "s");
    }

}