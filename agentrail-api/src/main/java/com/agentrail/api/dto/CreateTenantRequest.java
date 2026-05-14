package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank String name
) {}
