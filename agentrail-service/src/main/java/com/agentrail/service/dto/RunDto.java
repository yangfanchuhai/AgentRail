package com.agentrail.service.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RunDto(
        UUID id,
        UUID workflowVersionId,
        String temporalWorkflowId,
        String status,
        Map<String, Object> input,
        Map<String, Object> output,
        UUID currentNodeId,
        String startedBy,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {}
