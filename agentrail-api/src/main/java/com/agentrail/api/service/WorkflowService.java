package com.agentrail.api.service;

import com.agentrail.api.dto.*;
import com.agentrail.api.entity.*;
import com.agentrail.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository definitionRepo;
    private final WorkflowVersionRepository versionRepo;
    private final GraphSpecRepository graphSpecRepo;
    private final NodeSpecRepository nodeSpecRepo;
    private final EdgeSpecRepository edgeSpecRepo;
    private final WorkflowManifestRepository manifestRepo;

    // ── Definition CRUD ──

    @Transactional
    public WorkflowDefinitionDto createDefinition(UUID workspaceId, CreateWorkflowDefinitionRequest request) {
        if (definitionRepo.findByWorkspaceIdAndKey(workspaceId, request.key()).isPresent()) {
            throw new IllegalArgumentException("Workflow key already exists: " + request.key());
        }
        WorkflowDefinition def = WorkflowDefinition.builder()
                .workspaceId(workspaceId)
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .owner(request.owner())
                .tags(request.tags() != null ? request.tags() : List.of())
                .build();
        def = definitionRepo.save(def);
        return toDefinitionDto(def);
    }

    public List<WorkflowDefinitionDto> listDefinitions(UUID workspaceId) {
        return definitionRepo.findByWorkspaceId(workspaceId).stream()
                .map(this::toDefinitionDto)
                .toList();
    }

    public WorkflowDefinitionDto getDefinition(UUID workspaceId, UUID definitionId) {
        WorkflowDefinition def = findDefinition(workspaceId, definitionId);
        return toDefinitionDto(def);
    }

    @Transactional
    public void deleteDefinition(UUID workspaceId, UUID definitionId) {
        WorkflowDefinition def = findDefinition(workspaceId, definitionId);
        definitionRepo.delete(def);
    }

    // ── Version with Graph ──

    @Transactional
    public WorkflowVersionDto createVersionWithGraph(UUID workspaceId, UUID definitionId,
                                                      CreateWorkflowVersionWithGraphRequest request) {
        WorkflowDefinition def = findDefinition(workspaceId, definitionId);

        // Check version uniqueness
        if (versionRepo.findByWorkflowDefinitionIdAndVersion(definitionId, request.version()).isPresent()) {
            throw new IllegalArgumentException("Version already exists: " + request.version());
        }

        // Build graph spec
        GraphSpecDto graphReq = request.graph();
        GraphSpec graph = GraphSpec.builder()
                .entryNodeId(graphReq != null ? graphReq.entryNodeId() : null)
                .inputSchema(graphReq != null ? graphReq.inputSchema() : null)
                .outputSchema(graphReq != null ? graphReq.outputSchema() : null)
                .stateSchema(graphReq != null ? graphReq.stateSchema() : null)
                .build();
        graph = graphSpecRepo.save(graph);

        // Build node specs
        if (request.nodes() != null) {
            for (NodeSpecDto nodeDto : request.nodes()) {
                NodeSpec node = NodeSpec.builder()
                        .graphSpecId(graph.getId())
                        .key(nodeDto.key())
                        .name(nodeDto.name())
                        .type(nodeDto.type())
                        .executorRef(nodeDto.executorRef())
                        .config(nodeDto.config() != null ? nodeDto.config() : Map.of())
                        .inputMapping(nodeDto.inputMapping())
                        .outputMapping(nodeDto.outputMapping())
                        .checkpointPolicy(nodeDto.checkpointPolicy())
                        .transitionStrategy(nodeDto.transitionStrategy())
                        .build();
                nodeSpecRepo.save(node);
            }
        }

        // Build edge specs
        if (request.edges() != null) {
            for (EdgeSpecDto edgeDto : request.edges()) {
                EdgeSpec edge = EdgeSpec.builder()
                        .sourceNodeId(edgeDto.sourceNodeId())
                        .targetNodeId(edgeDto.targetNodeId())
                        .condition(edgeDto.condition())
                        .priority(edgeDto.priority() != null ? edgeDto.priority() : 0)
                        .label(edgeDto.label())
                        .build();
                edgeSpecRepo.save(edge);
            }
        }

        // Compute checksum
        String checksum = computeChecksum(def, request);

        // Create version
        WorkflowVersion version = WorkflowVersion.builder()
                .workflowDefinitionId(definitionId)
                .version(request.version())
                .changeSummary(request.changeSummary())
                .checksum(checksum)
                .build();
        version = versionRepo.save(version);

        // Create manifest
        WorkflowManifest manifest = WorkflowManifest.builder()
                .workflowVersionId(version.getId())
                .graphSpecId(graph.getId())
                .build();
        manifest = manifestRepo.save(manifest);

        // Link manifest back to version
        version.setManifestId(manifest.getId());
        versionRepo.save(version);

        return toVersionDto(version);
    }

    public List<WorkflowVersionDto> listVersions(UUID workspaceId, UUID definitionId) {
        findDefinition(workspaceId, definitionId); // verify access
        return versionRepo.findByWorkflowDefinitionIdOrderByCreatedAtDesc(definitionId).stream()
                .map(this::toVersionDto)
                .toList();
    }

    public WorkflowVersionDto getVersion(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        WorkflowVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));
        return toVersionDto(version);
    }

    // ── Publish ──

    @Transactional
    public WorkflowVersionDto publishVersion(UUID workspaceId, UUID definitionId,
                                              UUID versionId, String publishedBy) {
        findDefinition(workspaceId, definitionId);
        WorkflowVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        if (!"draft".equals(version.getStatus())) {
            throw new IllegalStateException("Only draft versions can be published. Current: " + version.getStatus());
        }

        // Validate: must have manifest with graph
        WorkflowManifest manifest = manifestRepo.findByWorkflowVersionId(versionId)
                .orElseThrow(() -> new IllegalStateException("Version has no manifest"));

        List<NodeSpec> nodes = nodeSpecRepo.findByGraphSpecId(manifest.getGraphSpecId());
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Graph has no nodes");
        }

        // Validate entry node exists
        GraphSpec graph = graphSpecRepo.findById(manifest.getGraphSpecId())
                .orElseThrow(() -> new IllegalStateException("Graph not found"));
        if (graph.getEntryNodeId() == null) {
            throw new IllegalStateException("Graph has no entry node");
        }

        version.setStatus("published");
        version.setPublishedBy(publishedBy);
        version.setPublishedAt(java.time.OffsetDateTime.now());
        version = versionRepo.save(version);

        // Update definition lifecycle
        WorkflowDefinition def = definitionRepo.findById(definitionId).get();
        def.setLifecycleStatus("active");
        definitionRepo.save(def);

        return toVersionDto(version);
    }

    // ── Graph Query ──

    public GraphSpecDto getGraph(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        WorkflowManifest manifest = manifestRepo.findByWorkflowVersionId(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Manifest not found"));
        GraphSpec graph = graphSpecRepo.findById(manifest.getGraphSpecId())
                .orElseThrow(() -> new IllegalArgumentException("Graph not found"));
        return new GraphSpecDto(graph.getEntryNodeId(), graph.getInputSchema(),
                graph.getOutputSchema(), graph.getStateSchema());
    }

    public List<NodeSpecDto> getNodes(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        WorkflowManifest manifest = manifestRepo.findByWorkflowVersionId(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Manifest not found"));
        return nodeSpecRepo.findByGraphSpecId(manifest.getGraphSpecId()).stream()
                .map(this::toNodeSpecDto)
                .toList();
    }

    public List<EdgeSpecDto> getEdges(UUID workspaceId, UUID definitionId, UUID versionId) {
        findDefinition(workspaceId, definitionId);
        WorkflowManifest manifest = manifestRepo.findByWorkflowVersionId(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Manifest not found"));
        List<NodeSpec> nodes = nodeSpecRepo.findByGraphSpecId(manifest.getGraphSpecId());
        List<UUID> nodeIds = nodes.stream().map(NodeSpec::getId).toList();
        return edgeSpecRepo.findBySourceNodeIdIn(nodeIds).stream()
                .map(this::toEdgeSpecDto)
                .toList();
    }

    // ── Helpers ──

    private WorkflowDefinition findDefinition(UUID workspaceId, UUID definitionId) {
        WorkflowDefinition def = definitionRepo.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + definitionId));
        if (!def.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Workflow does not belong to workspace: " + workspaceId);
        }
        return def;
    }

    private String computeChecksum(WorkflowDefinition def, CreateWorkflowVersionWithGraphRequest req) {
        try {
            String raw = def.getId() + ":" + req.version() + ":" + req.graph() + ":" + req.nodes();
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    private WorkflowDefinitionDto toDefinitionDto(WorkflowDefinition def) {
        return new WorkflowDefinitionDto(def.getId(), def.getWorkspaceId(), def.getKey(),
                def.getName(), def.getDescription(), def.getOwner(), def.getTags(),
                def.getLifecycleStatus(), def.getCreatedAt(), def.getUpdatedAt());
    }

    private WorkflowVersionDto toVersionDto(WorkflowVersion v) {
        return new WorkflowVersionDto(v.getId(), v.getWorkflowDefinitionId(), v.getVersion(),
                v.getStatus(), v.getManifestId(), v.getChecksum(), v.getChangeSummary(),
                v.getPublishedBy(), v.getPublishedAt(), v.getCreatedAt());
    }

    private NodeSpecDto toNodeSpecDto(NodeSpec n) {
        return new NodeSpecDto(n.getId(), n.getKey(), n.getName(), n.getType(),
                n.getExecutorRef(), n.getConfig(), n.getInputMapping(), n.getOutputMapping(),
                n.getCheckpointPolicy(), n.getTransitionStrategy());
    }

    private EdgeSpecDto toEdgeSpecDto(EdgeSpec e) {
        return new EdgeSpecDto(e.getId(), e.getSourceNodeId(), e.getTargetNodeId(),
                e.getCondition(), e.getPriority(), e.getLabel());
    }
}
