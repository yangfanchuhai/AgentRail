package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentDefinitionRequest(
        @NotBlank String key,
        @NotBlank String name,
        String description
) {}
