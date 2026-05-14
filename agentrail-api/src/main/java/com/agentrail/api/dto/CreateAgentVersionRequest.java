package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record CreateAgentVersionRequest(
        @NotBlank String version,
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
        Map<String, Object> outputSchema
) {}
