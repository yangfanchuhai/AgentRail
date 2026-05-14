package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record CreateToolVersionRequest(
        @NotBlank String version,
        @NotBlank String type,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        String connectionRef,
        String mcpServerRef,
        String mcpToolName,
        List<String> permissionScope,
        @NotBlank String sideEffectLevel,
        Map<String, Object> idempotencyPolicy
) {}
