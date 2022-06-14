package eu.wdaqua.qanary.g_answer_wrapper;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class RestTemplateCacheLiveTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("g_answer.endpoint.url", "http://some-ganswer-endpoint-url.com/endpoint");
        System.setProperty("qanary.webservicecalls.cache.specs",
                "maximumSize=1000,expireAfterAccess=" + TestGAnswerQueryBuilder.MAX_TIME_SPAN_SECONDS + "s");
    }

}