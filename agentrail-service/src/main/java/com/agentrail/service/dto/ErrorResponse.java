package com.agentrail.service.dto;

import java.util.UUID;

public record ErrorResponse(
        String error,
        String message
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }
}
