package com.agentrail.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateWorkflowVersionWithGraphRequest(
        @NotBlank String version,
        String changeSummary,
        GraphSpecDto graph,
        List<NodeSpecDto> nodes,
        List<EdgeSpecDto> edges
) {}
