package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowVersionRequest(
        @NotBlank String version,
        String changeSummary
) {}
