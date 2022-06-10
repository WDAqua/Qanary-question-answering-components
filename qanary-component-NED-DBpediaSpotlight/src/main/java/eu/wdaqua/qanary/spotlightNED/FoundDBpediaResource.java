package eu.wdaqua.qanary.spotlightNED;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.JsonElement;

/**
 * 
 * data object to be initialized by the DBpedia Spotlight service response  
 *
 */
public class FoundDBpediaResource {
	public int begin;
	public int end;
	public URI resource;
	public int support;
	public double similarityScore;

	public FoundDBpediaResource(String surfaceForm, int offset, double similarityScore, int support, URI resource) {
		this.begin = offset;
		this.end = offset + surfaceForm.length();
		this.similarityScore = similarityScore;
		this.support = support;
		this.resource = resource;
	}

	public FoundDBpediaResource(JsonElement jsonElement) throws URISyntaxException {
		this(jsonElement.getAsJsonObject().get("@surfaceForm").getAsString(),
				jsonElement.getAsJsonObject().get("@offset").getAsInt(),
				jsonElement.getAsJsonObject().get("@similarityScore").getAsDouble(),
				jsonElement.getAsJsonObject().get("@support").getAsInt(),
				new URI(jsonElement.getAsJsonObject().get("@URI").getAsString()));
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}

	public URI getResource() {
		return resource;
	}

	public int getSupport() {
		return support;
	}

	public double getSimilarityScore() {
		return similarityScore;
	}
}
