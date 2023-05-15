package eu.wdaqua.qanary.component.platypuswrapper.qb.messages;

public class DataNotProcessableException extends Exception {
    private static final long serialVersionUID = 1L;

    public DataNotProcessableException() {
        super("Data cannot be processed as either 'literal', 'resource' or 'boolean'.");
    }
}

