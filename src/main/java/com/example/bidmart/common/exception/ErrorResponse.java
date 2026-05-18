package com.example.bidmart.common.exception;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp
) {}
