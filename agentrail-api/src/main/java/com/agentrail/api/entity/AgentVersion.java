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
@Table(name = "agent_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersion {

    @Id
    private UUID id;

    @Column(name = "agent_definition_id", nullable = false)
    private UUID agentDefinitionId;

    @Column(nullable = false)
    private String version;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Column(name = "system_instruction", columnDefinition = "TEXT")
    private String systemInstruction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_refs", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> promptRefs = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_refs", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> skillRefs = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_refs", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> toolRefs = List.of();

    @Column(name = "model_policy_ref")
    private String modelPolicyRef;

    @Column(name = "planning_mode", length = 50)
    private String planningMode;

    @Column(name = "max_iterations")
    private Integer maxIterations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stop_condition", columnDefinition = "jsonb")
    private Map<String, Object> stopCondition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_schema", columnDefinition = "jsonb")
    private Map<String, Object> outputSchema;

    @Column(nullable = false)
    @Builder.Default
    private String status = "draft";

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
