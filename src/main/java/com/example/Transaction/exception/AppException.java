package com.example.Transaction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final String messageKey;
    private final HttpStatus status;

    public AppException(String messageKey, HttpStatus status) {
        this.messageKey = messageKey;
        this.status = status;
    }
}
