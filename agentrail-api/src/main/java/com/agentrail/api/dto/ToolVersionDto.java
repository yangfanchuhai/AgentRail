package com.agentrail.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ToolVersionDto(
        UUID id,
        UUID toolDefinitionId,
        String version,
        String type,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        String connectionRef,
        String mcpServerRef,
        String mcpToolName,
        List<String> permissionScope,
        String sideEffectLevel,
        Map<String, Object> idempotencyPolicy,
        String status,
        OffsetDateTime createdAt
) {}
