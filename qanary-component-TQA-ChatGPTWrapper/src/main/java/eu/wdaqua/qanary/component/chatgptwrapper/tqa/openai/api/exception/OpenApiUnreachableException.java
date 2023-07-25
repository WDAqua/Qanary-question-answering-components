package eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception;

import java.io.Serial;

public class OpenApiUnreachableException extends Exception{
    @Serial
    private static final long serialVersionUID = 515862938252669698L;

    public OpenApiUnreachableException(String message) {
        super(message);
    }
}
