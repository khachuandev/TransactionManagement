package com.example.Transaction.exception;

public class AESProcessingException extends RuntimeException {
    public AESProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
