package com.agentrail.api.dto;

import java.util.Map;
import java.util.UUID;

public record NodeSpecDto(
        UUID id,
        String key,
        String name,
        String type,
        Map<String, Object> executorRef,
        Map<String, Object> config,
        Map<String, Object> inputMapping,
        Map<String, Object> outputMapping,
        Map<String, Object> checkpointPolicy,
        String transitionStrategy
) {}
