package com.agentrail.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "workflow_manifests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowManifest {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "graph_spec_id", nullable = false)
    private UUID graphSpecId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agent_bindings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> agentBindings = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_bindings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> skillBindings = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_bindings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> toolBindings = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_bindings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> modelBindings = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_bindings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> promptBindings = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_bindings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> policyBindings = List.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onPersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
