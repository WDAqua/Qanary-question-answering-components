package eu.wdaqua.qanary.component.qanswer.qbe.messages;

import java.net.URI;
import java.util.List;

public class ProcessedResult {
	
	private final String type;
	private final List<String> values;
	private final URI datatype;

	public ProcessedResult(List<String> values, String type, URI datatype) {
		this.type = type;
		this.values = values;
		this.datatype = datatype;
	}
	
	public String getType() {
		return type;
	}

	public URI getDatatype() {
		return datatype;
	}

	public List<String> getValues() {
		return values;
	}	
}
