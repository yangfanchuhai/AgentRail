package com.agentrail.api.dto;

import java.util.Map;

public record GraphSpecDto(
        String entryNodeId,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Map<String, Object> stateSchema
) {}
