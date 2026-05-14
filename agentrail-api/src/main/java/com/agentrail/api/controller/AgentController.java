package com.agentrail.api.controller;

import com.agentrail.api.dto.*;
import com.agentrail.api.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentDefinitionDto createDefinition(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateAgentDefinitionRequest request) {
        return service.createDefinition(workspaceId, request);
    }

    @GetMapping
    public List<AgentDefinitionDto> listDefinitions(@PathVariable UUID workspaceId) {
        return service.listDefinitions(workspaceId);
    }

    @GetMapping("/{definitionId}")
    public AgentDefinitionDto getDefinition(
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
    public AgentVersionDto createVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @Valid @RequestBody CreateAgentVersionRequest request) {
        return service.createVersion(workspaceId, definitionId, request);
    }

    @GetMapping("/{definitionId}/versions")
    public List<AgentVersionDto> listVersions(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId) {
        return service.listVersions(workspaceId, definitionId);
    }

    @GetMapping("/{definitionId}/versions/{versionId}")
    public AgentVersionDto getVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId) {
        return service.getVersion(workspaceId, definitionId, versionId);
    }

    @PostMapping("/{definitionId}/versions/{versionId}/publish")
    public AgentVersionDto publishVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID definitionId,
            @PathVariable UUID versionId) {
        return service.publishVersion(workspaceId, definitionId, versionId);
    }
}
