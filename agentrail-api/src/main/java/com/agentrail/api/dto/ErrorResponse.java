package com.agentrail.api.dto;

import java.util.UUID;

public record ErrorResponse(
        String error,
        String message,
        String detail
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null);
    }

    public static ErrorResponse of(String error, String message, String detail) {
        return new ErrorResponse(error, message, detail);
    }
}
