package com.sun.dex.core.exception;

public class NoSpaceException extends Exception{
    public NoSpaceException() {
    }

    public NoSpaceException(String message) {
        super(message);
    }

    public NoSpaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSpaceException(Throwable cause) {
        super(cause);
    }

    public NoSpaceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
