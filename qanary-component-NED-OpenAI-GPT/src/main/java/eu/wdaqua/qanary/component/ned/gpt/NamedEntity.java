package eu.wdaqua.qanary.component.ned.gpt;

import java.net.URI;

/**
 * data of named entity including the resource linking
 */
public class NamedEntity {

	final int start;
	final int end;
	final String string;
	final URI resource;

	public NamedEntity(int start, int end, String string, URI resource) {
		this.start = start;
		this.end = end;
		this.string = string;
		this.resource = resource;
	}

	public NamedEntity(NamedEntity namedEntity, URI uri) {
		this.start = namedEntity.getStart();
		this.end = namedEntity.getEnd();
		this.string = namedEntity.getString();
		this.resource = uri;
	}

	public int getStart() {
		return start;
	}

	public String getStartAsString() {
		return String.format("%d", this.getStart());
	}

	public int getEnd() {
		return end;
	}

	public String getEndAsString() {
		return String.format("%d", this.getEnd());
	}

	public String getString() {
		return string;
	}

	public URI getResource() {
		return resource;
	}

	public String toString() {
		return String.format("%s found at (%d,%d) and linked to %s", this.getString(), this.getStart(), this.getEnd(),
				this.getResource());
	}

}
