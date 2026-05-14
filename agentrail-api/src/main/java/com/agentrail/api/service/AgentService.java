package com.agentrail.api.service;

import com.agentrail.api.dto.*;
import com.agentrail.api.entity.AgentDefinition;
import com.agentrail.api.entity.AgentVersion;
import com.agentrail.api.repository.AgentDefinitionRepository;
import com.agentrail.api.repository.AgentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentDefinitionRepository definitionRepo;
    private final AgentVersionRepository versionRepo;

    // ── Definition ──

    @Transactional
    public AgentDefinitionDto createDefinition(UUID workspaceId, CreateAgentDefinitionRequest request) {
        if (definitionRepo.findByWorkspaceIdAndKey(workspaceId, request.key()).isPresent()) {
            throw new IllegalArgumentException("Agent key already exists: " + request.key());
        }
        AgentDefinition def = AgentDefinition.builder()
                .workspaceId(workspaceId)
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .build();
        def = definitionRepo.save(def);
        return toDefinitionDto(def);
    }

    public List<AgentDefinitionDto> listDefinitions(UUID workspaceId) {
        return definitionRepo.findByWorkspaceId(workspaceId).stream()
                .map(this::toDefinitionDto)
                .toList();
    }

    public AgentDefinitionDto getDefinition(UUID workspaceId, UUID definitionId) {
        return toDefinitionDto(findDefinition(workspaceId, definitionId));
    }

    @Transactional
    public void deleteDefinition(UUID workspaceId, UUID definitionId) {
        AgentDefinition def = findDefinition(workspaceId, definitionId);
        definitionRepo.delete(def);
    }

    // ── Version ──

    @Transactional
    public AgentVersionDto createVersion(UUID workspaceId, UUID definitionId,
                                          CreateAgentVersionRequest request) {
        findDefinition(workspaceId, definitionId);
        if (versionRepo.findByAgentDefinitionIdAndVersion(definitionId, request.version()).isPresent()) {
            throw new IllegalArgumentException("Agent version already exists: " + request.version());
        }
        AgentVersion version = AgentVersion.builder()
                .agentDefinitionId(definitionId)
                .version(request.version())
                .role(request.role())
                .goal(request.goal())
                .systemInstruction(request.systemInstruction())
                .promptRefs(request.promptRefs() != null ? request.promptRefs() : List.of())
                .skillRefs(request.skillRefs() != null ? request.skillRefs() : List.of())
                .toolRefs(request.toolRefs() != null ? request.toolRefs() : List.of())
                .modelPolicyRef(request.modelPolicyRef())
                .planningMode(request.planningMode())
                .maxIterations(request.maxIterations())
                .stopCondition(request.stopCondition())
                .outputSchema(request.outputSchema())
                .build();
        version = versionRepo.save(version);
        return toVersionDto(version);
    }

    public List<AgentVersionDto> listVersions(UUID workspaceId, UUID definitionId) {
        findDefinition(workspaceId, definitionId);
        return versionRepo.findByAgentDefinitionIdOrderByCreatedAtDesc(definitionId).stream()
                .map(this::toVersionDto)
                .toList();
    }

    public AgentVersionDto getVersion(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        AgentVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent version not found: " + versionId));
        return toVersionDto(version);
    }

    @Transactional
    public AgentVersionDto publishVersion(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        AgentVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent version not found: " + versionId));
        if (!"draft".equals(version.getStatus())) {
            throw new IllegalStateException("Only draft versions can be published");
        }
        version.setStatus("published");
        version = versionRepo.save(version);

        // Update definition lifecycle
        AgentDefinition def = definitionRepo.findById(definitionId).get();
        def.setLifecycleStatus("active");
        definitionRepo.save(def);

        return toVersionDto(version);
    }

    // ── Helpers ──

    private AgentDefinition findDefinition(UUID workspaceId, UUID definitionId) {
        AgentDefinition def = definitionRepo.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent definition not found: " + definitionId));
        if (!def.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Agent does not belong to workspace: " + workspaceId);
        }
        return def;
    }

    private AgentDefinitionDto toDefinitionDto(AgentDefinition def) {
        return new AgentDefinitionDto(def.getId(), def.getWorkspaceId(), def.getKey(),
                def.getName(), def.getDescription(), def.getLifecycleStatus(),
                def.getCreatedAt(), def.getUpdatedAt());
    }

    private AgentVersionDto toVersionDto(AgentVersion v) {
        return new AgentVersionDto(v.getId(), v.getAgentDefinitionId(), v.getVersion(),
                v.getRole(), v.getGoal(), v.getSystemInstruction(),
                v.getPromptRefs(), v.getSkillRefs(), v.getToolRefs(),
                v.getModelPolicyRef(), v.getPlanningMode(), v.getMaxIterations(),
                v.getStopCondition(), v.getOutputSchema(), v.getStatus(), v.getCreatedAt());
    }
}
