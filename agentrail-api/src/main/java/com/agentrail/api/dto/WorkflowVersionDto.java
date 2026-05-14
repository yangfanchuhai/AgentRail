package com.agentrail.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkflowVersionDto(
        UUID id,
        UUID workflowDefinitionId,
        String version,
        String status,
        UUID manifestId,
        String checksum,
        String changeSummary,
        String publishedBy,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
) {}
