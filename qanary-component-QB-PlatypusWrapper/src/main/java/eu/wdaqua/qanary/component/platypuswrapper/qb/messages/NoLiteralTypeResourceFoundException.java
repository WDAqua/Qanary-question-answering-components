package eu.wdaqua.qanary.component.platypuswrapper.qb.messages;

public class NoLiteralTypeResourceFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoLiteralTypeResourceFoundException(String type) {
        super("'" + type + "' found, but no URI can be assigned to this literal datatype.");
    }
}

