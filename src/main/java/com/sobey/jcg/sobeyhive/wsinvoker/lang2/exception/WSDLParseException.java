package com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception;

/**
 * Created by WX on 2015/10/10.
 */
public class WSDLParseException extends RuntimeException {
    public WSDLParseException() {
    }

    public WSDLParseException(String message) {
        super(message);
    }

    public WSDLParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public WSDLParseException(Throwable cause) {
        super(cause);
    }

    public WSDLParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
