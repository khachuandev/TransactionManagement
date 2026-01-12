package com.example.Transaction.exception;

public class RSAKeyLoadException extends RuntimeException{
    public RSAKeyLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public RSAKeyLoadException(String message) {
        super(message);
    }
}
