package com.agentrail.api.controller;

import com.agentrail.api.dto.*;
import com.agentrail.api.entity.Tenant;
import com.agentrail.api.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tenant createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return service.createTenant(request);
    }

    @GetMapping
    public List<Tenant> listTenants() {
        return service.listTenants();
    }

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable UUID tenantId) {
        return service.getTenant(tenantId);
    }

    // ── Workspaces ──

    @PostMapping("/{tenantId}/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceDto createWorkspace(@PathVariable UUID tenantId,
                                         @Valid @RequestBody CreateWorkspaceRequest request) {
        return service.createWorkspace(tenantId, request);
    }

    @GetMapping("/{tenantId}/workspaces")
    public List<WorkspaceDto> listWorkspaces(@PathVariable UUID tenantId) {
        return service.listWorkspaces(tenantId);
    }

    @GetMapping("/{tenantId}/workspaces/{workspaceId}")
    public WorkspaceDto getWorkspace(@PathVariable UUID tenantId,
                                      @PathVariable UUID workspaceId) {
        return service.getWorkspace(tenantId, workspaceId);
    }
}
