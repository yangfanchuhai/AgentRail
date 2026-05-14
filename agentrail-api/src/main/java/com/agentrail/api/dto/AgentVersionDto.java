package com.agentrail.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentVersionDto(
        UUID id,
        UUID agentDefinitionId,
        String version,
        String role,
        String goal,
        String systemInstruction,
        List<Map<String, Object>> promptRefs,
        List<Map<String, Object>> skillRefs,
        List<Map<String, Object>> toolRefs,
        String modelPolicyRef,
        String planningMode,
        Integer maxIterations,
        Map<String, Object> stopCondition,
        Map<String, Object> outputSchema,
        String status,
        OffsetDateTime createdAt
) {}
