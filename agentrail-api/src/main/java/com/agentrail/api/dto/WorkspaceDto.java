package com.agentrail.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceDto(
        UUID id,
        UUID tenantId,
        String name,
        String slug,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
