package eu.wdaqua.qanary.component.ned.gpt.openai;

import java.util.Arrays;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * configure the caching of the OpenAI API calls
 */
@Configuration
@EnableCaching
public class OpenAiApiCachingConfig {
	public static final String PROMPT_RESPONSE_CACHE_NAME = "promptresponses";

	@Bean
	@Primary
	public CacheManager OpenAiApiCacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache(PROMPT_RESPONSE_CACHE_NAME)));
		return cacheManager;
	}
}