package com.agentrail.api.dto;

import java.util.Map;
import java.util.UUID;

public record EdgeSpecDto(
        UUID id,
        UUID sourceNodeId,
        UUID targetNodeId,
        Map<String, Object> condition,
        Integer priority,
        String label
) {}
