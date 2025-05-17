package com.example.chatbot.exception;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
    int status,
    String error,
    String errorCode,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            null,
            "success",
            data
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
            errorCode.getHttpStatus().value(),
            errorCode.getHttpStatus().getReasonPhrase(),
            errorCode.getCode(),
            errorCode.getMessage(),
            null
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
            errorCode.getHttpStatus().value(),
            errorCode.getHttpStatus().getReasonPhrase(),
            errorCode.getCode(),
            message,
            null
        );
    }
} 