package com.agentrail.api.service;

import com.agentrail.api.dto.*;
import com.agentrail.api.entity.ToolDefinition;
import com.agentrail.api.entity.ToolVersion;
import com.agentrail.api.repository.ToolDefinitionRepository;
import com.agentrail.api.repository.ToolVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToolService {

    private final ToolDefinitionRepository definitionRepo;
    private final ToolVersionRepository versionRepo;

    // ── Definition ──

    @Transactional
    public ToolDefinitionDto createDefinition(UUID workspaceId, CreateToolDefinitionRequest request) {
        if (definitionRepo.findByWorkspaceIdAndKey(workspaceId, request.key()).isPresent()) {
            throw new IllegalArgumentException("Tool key already exists: " + request.key());
        }
        ToolDefinition def = ToolDefinition.builder()
                .workspaceId(workspaceId)
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .build();
        def = definitionRepo.save(def);
        return toDefinitionDto(def);
    }

    public List<ToolDefinitionDto> listDefinitions(UUID workspaceId) {
        return definitionRepo.findByWorkspaceId(workspaceId).stream()
                .map(this::toDefinitionDto)
                .toList();
    }

    public ToolDefinitionDto getDefinition(UUID workspaceId, UUID definitionId) {
        return toDefinitionDto(findDefinition(workspaceId, definitionId));
    }

    @Transactional
    public void deleteDefinition(UUID workspaceId, UUID definitionId) {
        ToolDefinition def = findDefinition(workspaceId, definitionId);
        definitionRepo.delete(def);
    }

    // ── Version ──

    @Transactional
    public ToolVersionDto createVersion(UUID workspaceId, UUID definitionId,
                                         CreateToolVersionRequest request) {
        findDefinition(workspaceId, definitionId);
        if (versionRepo.findByToolDefinitionIdAndVersion(definitionId, request.version()).isPresent()) {
            throw new IllegalArgumentException("Tool version already exists: " + request.version());
        }
        ToolVersion version = ToolVersion.builder()
                .toolDefinitionId(definitionId)
                .version(request.version())
                .type(request.type())
                .inputSchema(request.inputSchema())
                .outputSchema(request.outputSchema())
                .connectionRef(request.connectionRef())
                .mcpServerRef(request.mcpServerRef())
                .mcpToolName(request.mcpToolName())
                .permissionScope(request.permissionScope())
                .sideEffectLevel(request.sideEffectLevel())
                .idempotencyPolicy(request.idempotencyPolicy())
                .build();
        version = versionRepo.save(version);
        return toVersionDto(version);
    }

    public List<ToolVersionDto> listVersions(UUID workspaceId, UUID definitionId) {
        findDefinition(workspaceId, definitionId);
        return versionRepo.findByToolDefinitionIdOrderByCreatedAtDesc(definitionId).stream()
                .map(this::toVersionDto)
                .toList();
    }

    public ToolVersionDto getVersion(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        ToolVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Tool version not found: " + versionId));
        return toVersionDto(version);
    }

    @Transactional
    public ToolVersionDto publishVersion(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        ToolVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Tool version not found: " + versionId));
        if (!"draft".equals(version.getStatus())) {
            throw new IllegalStateException("Only draft versions can be published");
        }
        version.setStatus("published");
        version = versionRepo.save(version);

        ToolDefinition def = definitionRepo.findById(definitionId).get();
        def.setLifecycleStatus("active");
        definitionRepo.save(def);

        return toVersionDto(version);
    }

    // ── Helpers ──

    private ToolDefinition findDefinition(UUID workspaceId, UUID definitionId) {
        ToolDefinition def = definitionRepo.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Tool definition not found: " + definitionId));
        if (!def.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Tool does not belong to workspace: " + workspaceId);
        }
        return def;
    }

    private ToolDefinitionDto toDefinitionDto(ToolDefinition def) {
        return new ToolDefinitionDto(def.getId(), def.getWorkspaceId(), def.getKey(),
                def.getName(), def.getDescription(), def.getLifecycleStatus(),
                def.getCreatedAt(), def.getUpdatedAt());
    }

    private ToolVersionDto toVersionDto(ToolVersion v) {
        return new ToolVersionDto(v.getId(), v.getToolDefinitionId(), v.getVersion(),
                v.getType(), v.getInputSchema(), v.getOutputSchema(),
                v.getConnectionRef(), v.getMcpServerRef(), v.getMcpToolName(),
                v.getPermissionScope(), v.getSideEffectLevel(), v.getIdempotencyPolicy(),
                v.getStatus(), v.getCreatedAt());
    }
}
