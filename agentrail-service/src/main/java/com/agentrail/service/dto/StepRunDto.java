package com.agentrail.service.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record StepRunDto(
        UUID id,
        UUID runId,
        UUID nodeSpecId,
        String status,
        Integer attempt,
        Map<String, Object> input,
        Map<String, Object> output,
        Map<String, Object> error,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {}
