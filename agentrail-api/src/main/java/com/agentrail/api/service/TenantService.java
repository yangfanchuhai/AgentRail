package com.agentrail.api.service;

import com.agentrail.api.dto.CreateTenantRequest;
import com.agentrail.api.dto.WorkspaceDto;
import com.agentrail.api.dto.CreateWorkspaceRequest;
import com.agentrail.api.entity.Tenant;
import com.agentrail.api.entity.Workspace;
import com.agentrail.api.repository.TenantRepository;
import com.agentrail.api.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .build();
        return tenantRepository.save(tenant);
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    // ── Workspace ──

    @Transactional
    public WorkspaceDto createWorkspace(UUID tenantId, CreateWorkspaceRequest request) {
        Tenant tenant = getTenant(tenantId);
        if (workspaceRepository.findByTenantIdAndSlug(tenantId, request.slug()).isPresent()) {
            throw new IllegalArgumentException("Workspace slug already exists: " + request.slug());
        }
        Workspace ws = Workspace.builder()
                .tenantId(tenantId)
                .name(request.name())
                .slug(request.slug())
                .build();
        ws = workspaceRepository.save(ws);
        return toWorkspaceDto(ws);
    }

    public List<WorkspaceDto> listWorkspaces(UUID tenantId) {
        return workspaceRepository.findByTenantId(tenantId).stream()
                .map(this::toWorkspaceDto)
                .toList();
    }

    public WorkspaceDto getWorkspace(UUID tenantId, UUID workspaceId) {
        Workspace ws = workspaceRepository.findById(workspaceId)
                .filter(w -> w.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        return toWorkspaceDto(ws);
    }

    private WorkspaceDto toWorkspaceDto(Workspace ws) {
        return new WorkspaceDto(ws.getId(), ws.getTenantId(), ws.getName(),
                ws.getSlug(), ws.getStatus(), ws.getCreatedAt(), ws.getUpdatedAt());
    }
}
