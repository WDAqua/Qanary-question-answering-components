package eu.wdaqua.qanary.component.qanswer.qbe;

import java.net.URI;

public class NamedEntity {
	private final URI namedEntityResource;
	private final int startPosition;
	private final int endPosition;
	private final Float score;

	public NamedEntity(URI namedEntityResource, int startPosition, int endPosition, Float score) {
		this.namedEntityResource = namedEntityResource;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.score = score;
	}

	public NamedEntity(URI namedEntityResource, int startPosition, String entity, Float score) {
		this.namedEntityResource = namedEntityResource;
		this.startPosition = startPosition;
		this.endPosition = startPosition + entity.length();
		this.score = score;
	}

	public URI getNamedEntityResource() {
		return namedEntityResource;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public Float getScore() {
		return score;
	}

	public String toString() {
		return namedEntityResource + " at (" + startPosition + "," + endPosition + ") with score " + score;
	}
}
