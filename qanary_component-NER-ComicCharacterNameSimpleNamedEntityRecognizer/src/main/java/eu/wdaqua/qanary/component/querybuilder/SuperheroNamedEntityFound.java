package eu.wdaqua.qanary.component.querybuilder;

/**
 * 
 * data container for holding the information of a found super hero name 
 *
 */
public class SuperheroNamedEntityFound {
	private final String superheroLabel;
	private final String resource;
	private final int beginIndex;
	private final int endIndex;

	public SuperheroNamedEntityFound(String superheroLabel, String resource, int beginIndex, int endIndex) {
		this.superheroLabel = superheroLabel;
		this.resource = resource;
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
	}
	
	public String getSuperheroLabel() {
		return this.superheroLabel;
	}

	public String getResource() {
		return resource;
	}

	public int getBeginIndex() {
		return this.beginIndex;
	}
	
	public int getEndIndex() {
		return this.endIndex;
	}

}
