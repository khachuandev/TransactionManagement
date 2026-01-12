package com.example.Transaction.dto.response;

import lombok.*;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiRes<T> {
    private int code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> ApiRes<T> success(T data) {
        return build(HttpStatus.OK.value(), "Success", data);
    }

    public static <T> ApiRes<T> created(T data) {
        return build(HttpStatus.CREATED.value(), "Created", data);
    }

    private static <T> ApiRes<T> build(int code, String message, T data) {
        return ApiRes.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiRes<T> error(String message) {
        return build(HttpStatus.BAD_REQUEST.value(), message, null);
    }
}
