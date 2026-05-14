package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateToolDefinitionRequest(
        @NotBlank String key,
        @NotBlank String name,
        String description
) {}
