package eu.wdaqua.qanary.component.ned.gpt.openai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import eu.wdaqua.qanary.component.ned.gpt.GptBasedNamedEntityDisambiguation;

/**
 * internal service wrapping the OpenAI API, includes a cache to reduce the API
 * calls
 */
@Component
public class OpenAiApiService {

	private final String token;

	private final short timeoutInSeconds;

	private final int maxToken;

	private static final Logger LOGGER = LoggerFactory.getLogger(GptBasedNamedEntityDisambiguation.class);

	private static int numberOfExecutedRequests = 0;

	public static int getNumberOfExecutedRequests() {
		return numberOfExecutedRequests;
	}

	private static void increaseNumberOfExecutedRequests() {
		numberOfExecutedRequests++;
	}

	public OpenAiApiService( //
			@Value("${openai.api.key}") String token, //
			@Value("${openai.api.timeout:30}") short timeoutInSeconds, //
			@Value("${openai.api.maxToken:255}") int maxToken //
	) throws MissingArgumentException {
		this.token = token;
		this.timeoutInSeconds = timeoutInSeconds;
		this.maxToken = maxToken;

		if (this.token.isBlank()) {
			LOGGER.error("The API token was empty: '{}'. Please set the configuration property 'openai.api.key'.",
					this.token);
			throw new MissingArgumentException("openai.api.key is not avaible.");
		}
	}

	@Cacheable(value = OpenAiApiCachingConfig.PROMPT_RESPONSE_CACHE_NAME)
	public List<ChatCompletionChoice> getCompletion(String prompt, String model)
			throws OpenAiApiFetchingServiceFailedException {
		try {
			OpenAiService service = new OpenAiService(token, Duration.ofSeconds(this.timeoutInSeconds));

			final List<ChatMessage> messages = new ArrayList<>();
			final ChatMessage message = new ChatMessage(ChatMessageRole.SYSTEM.value(), prompt);
			messages.add(message);
			ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model(model)
					.messages(messages).n(1).maxTokens(this.maxToken).logitBias(new HashMap<>()).build();

			ChatCompletionResult myChatCompletionResult = service.createChatCompletion(chatCompletionRequest);
			List<ChatCompletionChoice> choices = myChatCompletionResult.getChoices();

			LOGGER.warn("{}. API call was actually executed (no caching) computing {} choices with model {} : {}",
					getNumberOfExecutedRequests(), choices.size(), model, prompt);
			increaseNumberOfExecutedRequests();
			service.shutdownExecutor();

			return choices;

		} catch (Exception e) {
			e.printStackTrace();
			throw new OpenAiApiFetchingServiceFailedException(e.toString());
		}
	}

}
