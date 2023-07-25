package eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception;

import java.io.Serial;

public class MissingTokenException extends Exception{
    @Serial
    private static final long serialVersionUID = 7200296290328755734L;

    public MissingTokenException(String message) {
        super(message);
    }
}
