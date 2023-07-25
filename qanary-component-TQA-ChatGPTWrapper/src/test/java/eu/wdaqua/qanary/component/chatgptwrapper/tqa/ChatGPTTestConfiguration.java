package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import org.springframework.boot.test.context.TestConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@TestConfiguration
public class ChatGPTTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
    	
        System.setProperty("chatgpt.base.url", "https://api.openai.com");
        System.setProperty("chatGPT.getModels.url", "/v1/models");
        System.setProperty("chatGPT.getModelById.url", "/v1/models/");
        System.setProperty("chatGPT.createCompletions.url", "/v1/completions");

        System.setProperty("qanary.webservicecalls.cache.specs", "maximumSize=1000,expireAfterAccess=" + ChatGPTCacheTest.MAX_TIME_SPAN_SECONDS + "s");
    }

    /**
     * get the content from the defined file
     *
     * @return
     * @throws IOException
     */
    protected static String getStringFromFile(String filename) throws IOException {
        String path = ChatGPTTestConfiguration.class.getClassLoader().getResource(filename).getPath();

        return new String(Files.readAllBytes(Paths.get(path)));
    }
}