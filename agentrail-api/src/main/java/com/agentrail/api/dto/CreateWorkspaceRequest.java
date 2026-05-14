package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
        @NotBlank String name,
        @NotBlank String slug
) {}
