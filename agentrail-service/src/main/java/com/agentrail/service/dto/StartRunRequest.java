package com.agentrail.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record StartRunRequest(
        @NotNull UUID workflowVersionId,
        Map<String, Object> input
) {}
