package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateWorkflowDefinitionRequest(
        @NotBlank String key,
        @NotBlank String name,
        String description,
        String owner,
        List<String> tags
) {}
