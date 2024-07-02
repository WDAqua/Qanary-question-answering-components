package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import com.google.gson.JsonElement;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * data object to be initialized by the DBpedia Spotlight service response
 */
public class FoundDBpediaResource {
	public int begin;
	public int end;
	public URI resource;
	public int support;
	public double similarityScore;

	/**
	 * constructor
	 * 
	 * @param surfaceForm
	 * @param offset
	 * @param similarityScore
	 * @param support
	 * @param resource
	 */
	public FoundDBpediaResource(String surfaceForm, int offset, double similarityScore, int support, URI resource) {
		this.begin = offset;
		this.end = offset + surfaceForm.length();
		this.similarityScore = similarityScore;
		this.support = support;
		this.resource = resource;
	}

	
	/**
	 * constructor create from JSON
	 * 
	 * @param jsonElement
	 * @throws URISyntaxException
	 */
	public FoundDBpediaResource(JsonElement jsonElement) throws URISyntaxException {
		this(jsonElement.getAsJsonObject().get("surfaceForm").getAsString(),
				jsonElement.getAsJsonObject().get("offset").getAsInt(),
				jsonElement.getAsJsonObject().get("similarityScore").getAsDouble(),
				jsonElement.getAsJsonObject().get("support").getAsInt(),
				new URI(jsonElement.getAsJsonObject().get("URI").getAsString()));
	}

	/**
	 * get begin index
	 * 
	 * @return
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * get end index 
	 * 
	 * @return
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * get DBpedia resource
	 * 
	 * @return
	 */
	public URI getResource() {
		return resource;
	}

	/**
	 * get support score
	 * 
	 * @return
	 */
	public int getSupport() {
		return support;
	}
	
	/**
	 * get similarity score
	 * 
	 * @return
	 */
	public double getSimilarityScore() {
		return similarityScore;
	}
}
