package eu.wdaqua.qanary.component.platypuswrapper.qb.messages;

import java.net.URI;

public class ProcessedResult {

    private final URI datatype;
    private final String value; 

    public ProcessedResult(String value, URI datatype) {
        this.value = value;
        this.datatype = datatype;
    }

    public URI getDataType() {
        return datatype;
    }

    public String getValue() {
        return value; 
    }
}

