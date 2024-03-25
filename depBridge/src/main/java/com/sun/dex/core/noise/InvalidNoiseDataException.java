package com.sun.dex.core.noise;

public class InvalidNoiseDataException extends Exception{
    public InvalidNoiseDataException() {
    }

    public InvalidNoiseDataException(String message) {
        super(message);
    }

    public InvalidNoiseDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidNoiseDataException(Throwable cause) {
        super(cause);
    }

    public InvalidNoiseDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
