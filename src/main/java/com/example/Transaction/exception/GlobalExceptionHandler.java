package com.example.Transaction.exception;

import com.example.Transaction.config.Translator;
import com.example.Transaction.dto.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    // 1. Business Exception (custom AppException)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e){
        String localizedMessage = Translator.toLocale(e.getMessageKey());

        ErrorResponse response = ErrorResponse.builder()
                .code(e.getStatus().value())
                .message(localizedMessage)
                .build();

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    // 2. Validation Exception - Trả errors map chi tiết
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e){
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(Translator.toLocale("err.validation"))
                .errors(errors.isEmpty() ? null : errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // 3. ResponseStatusException Handler
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        String reason = e.getReason() != null ? Translator.toLocale(e.getReason()) : e.getReason();

        ErrorResponse response = ErrorResponse.builder()
                .code(e.getStatusCode().value())
                .message(reason)
                .build();

        return ResponseEntity.status(e.getStatusCode()).body(response);
    }

    // 4. Uncategorized Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);

        String message = Translator.toLocale("err.uncategorized");

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .build();

        return ResponseEntity.internalServerError().body(response);
    }
}
