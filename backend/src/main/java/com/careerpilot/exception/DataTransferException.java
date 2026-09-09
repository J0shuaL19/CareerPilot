package com.careerpilot.exception;

public class DataTransferException extends RuntimeException {

    public DataTransferException(String message) {
        super(message);
    }

    public DataTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
