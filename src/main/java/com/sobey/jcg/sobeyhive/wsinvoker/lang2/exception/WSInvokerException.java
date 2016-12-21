package com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception;

public class WSInvokerException extends Exception
{
    public WSInvokerException() {
    }

    public WSInvokerException(String fault){
		super(fault);
	}

    public WSInvokerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WSInvokerException(Throwable cause) {
        super(cause);
    }

    public WSInvokerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
