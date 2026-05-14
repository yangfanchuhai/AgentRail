package com.agentrail.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WorkflowDefinitionDto(
        UUID id,
        UUID workspaceId,
        String key,
        String name,
        String description,
        String owner,
        List<String> tags,
        String lifecycleStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
