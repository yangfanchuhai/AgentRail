package com.agentrail.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ToolDefinitionDto(
        UUID id,
        UUID workspaceId,
        String key,
        String name,
        String description,
        String lifecycleStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
