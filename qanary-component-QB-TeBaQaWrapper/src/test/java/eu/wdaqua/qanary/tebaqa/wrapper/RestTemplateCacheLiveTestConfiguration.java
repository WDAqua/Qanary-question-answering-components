package eu.wdaqua.qanary.tebaqa.wrapper;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class RestTemplateCacheLiveTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("tebaqa.endpoint.url", "https://tebaqa.demos.dice-research.org/qa-simple");
        System.setProperty("qanary.webservicecalls.cache.specs",
                "maximumSize=1000,expireAfterAccess=" + TestTeBaQAQueryBuilder.MAX_TIME_SPAN_SECONDS + "s");
    }

}