package eu.wdaqua.opentapiocaNED;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.JsonElement;

/**
 *
 * data object to be initialized by the Opentapioca service response
 *
 */
public class FoundWikidataResource {
	public int begin;
	public int end;
	public URI resource;
	public int support;
	public double similarityScore;

	public FoundWikidataResource(//
			String surfaceForm, //
			int offset, //
			double similarityScore, //
			int support, //
			URI resource) {

		this.begin = offset;
		this.end = offset + surfaceForm.length();
		this.similarityScore = similarityScore;
		this.support = support;
		this.resource = resource;
	}

	public FoundWikidataResource(JsonElement jsonElement) throws URISyntaxException {
		// TODO: implement NIF parsing
		this(jsonElement.getAsJsonObject().get("text").getAsString().substring(
					jsonElement.getAsJsonObject().get("start").getAsInt(), 
					jsonElement.getAsJsonObject().get("end").getAsInt()
					), //
				jsonElement.getAsJsonObject().get("start").getAsInt(),//
				jsonElement.getAsJsonObject().get("score").getAsDouble(), //
				0, //
				new URI("https://www.wikidata.org/wiki/"
				 + jsonElement.getAsJsonObject().get("id").getAsString())
				);
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
