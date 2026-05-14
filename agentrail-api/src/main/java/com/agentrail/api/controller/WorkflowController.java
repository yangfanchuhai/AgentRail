package com.agentrail.api.controller;

import com.agentrail.api.dto.*;
import com.agentrail.api.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService service;

    // ── Definition CRUD ──

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDefinitionDto createDefinition(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateWorkflowDefinitionRequest request) {
        return service.createDefinition(workspaceId, request);
    }

    @GetMapping
    public List<WorkflowDefinitionDto> listDefinitions(@PathVariable UUID workspaceId) {
        return service.listDefinitions(workspaceId);
    }

    @GetMapping("/{definitionId}")
    public WorkflowDefinitionDto getDefinition(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId) {
        return service.getDefinition(workspaceId, definitionId);
    }

    @DeleteMapping("/{definitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDefinition(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId) {
        service.deleteDefinition(workspaceId, definitionId);
    }

    // ── Versions ──

    @PostMapping("/{definitionId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowVersionDto createVersionWithGraph(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @Valid @RequestBody CreateWorkflowVersionWithGraphRequest request) {
        return service.createVersionWithGraph(workspaceId, definitionId, request);
    }

    @GetMapping("/{definitionId}/versions")
    public List<WorkflowVersionDto> listVersions(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId) {
        return service.listVersions(workspaceId, definitionId);
    }

    @GetMapping("/{definitionId}/versions/{versionId}")
    public WorkflowVersionDto getVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId) {
        return service.getVersion(workspaceId, definitionId, versionId);
    }

    // ── Publish ──

    @PostMapping("/{definitionId}/versions/{versionId}/publish")
    public WorkflowVersionDto publishVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String publishedBy) {
        return service.publishVersion(workspaceId, definitionId, versionId, publishedBy);
    }

    // ── Graph / Nodes / Edges ──

    @GetMapping("/{definitionId}/versions/{versionId}/graph")
    public GraphSpecDto getGraph(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId) {
        return service.getGraph(workspaceId, definitionId, versionId);
    }

    @GetMapping("/{definitionId}/versions/{versionId}/nodes")
    public List<NodeSpecDto> getNodes(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId) {
        return service.getNodes(workspaceId, definitionId, versionId);
    }

    @GetMapping("/{definitionId}/versions/{versionId}/edges")
    public List<EdgeSpecDto> getEdges(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId) {
        return service.getEdges(workspaceId, definitionId, versionId);
    }
}
