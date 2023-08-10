package eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theokanning.openai.completion.CompletionRequest;

public class MyCompletionRequest extends CompletionRequest {
    public JsonObject getAsJsonObject() {
        JsonObject json = new JsonObject();

        if (getModel() != null){
            json.addProperty("model", getModel());
        }

        if (getPrompt() != null){
            json.addProperty("prompt", getPrompt());
        }

        if (getSuffix() != null){
            json.addProperty("suffix", getSuffix());
        }

        if (getMaxTokens() != null){
            json.addProperty("max_tokens", getMaxTokens());
        }

        if (getTemperature() != null){
            json.addProperty("temperature", getTemperature());
        }

        if (getTopP() != null){
            json.addProperty("top_p", getTopP());
        }

        if (getN() != null){
            json.addProperty("n", getN());
        }

        if (getStream() != null){
            json.addProperty("stream", getStream());
        }

        if (getLogprobs() != null){
            json.addProperty("logprobs", getLogprobs());
        }

        if (getEcho() != null){
            json.addProperty("echo", getEcho());
        }

        if (getStop() != null){
            JsonArray stopArray = new JsonArray();

            for (String stop : getStop()){
                stopArray.add(stop);
            }

            json.add("stop", stopArray);
        }

        if (getPresencePenalty() != null){
            json.addProperty("presence_penalty", getPresencePenalty());
        }

        if (getFrequencyPenalty() != null){
            json.addProperty("frequency_penalty", getFrequencyPenalty());
        }

        if (getLogitBias() != null){
            JsonObject logitBiasObject = new JsonObject();

            for (String key : getLogitBias().keySet()){
                logitBiasObject.addProperty(key, getLogitBias().get(key));
            }

            json.add("logit_bias", logitBiasObject);
        }

        if (getUser() != null){
            json.addProperty("user", getUser());
        }

        return json;
    }
}
