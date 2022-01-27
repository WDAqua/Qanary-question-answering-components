package eu.wdaqua.qanary.falconNED;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * represents a wrapper of the DBpedia Spotlight service used as NED annotator
 * 
 * requirements: this Qanary service expects as input a textual question (that
 * is stored in the Qanary triplestore) written using English language
 * 
 * outcome: if DBpedia Spotlight has recognized named entities and was enabled
 * to link them to DBpedia, then this information is added to the Qanary
 * triplestore to be used by following services of this question answering
 * process
 *
 * @author Kuldeep Singh, Dennis Diefenbach, Andreas Both
 */

@Component
public class FalconNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(FalconNED.class);


	@Bean
	public CacheManagerCustomizer<ConcurrentMapCacheManager> getCacheManagerCustomizer() {
		logger.warn("getCacheManagerCustomizer");
		return new CacheManagerCustomizer<ConcurrentMapCacheManager>() {
			@Override
			public void customize(ConcurrentMapCacheManager cacheManager) {
				cacheManager.setAllowNullValues(false);
			}
		};
	}

	/**
	 * standard method for processing a message from the central Qanary component
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {



		return myQanaryMessage;
	}

}
