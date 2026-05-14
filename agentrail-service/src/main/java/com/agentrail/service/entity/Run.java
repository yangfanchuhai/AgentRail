package com.agentrail.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Run {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "temporal_workflow_id")
    private String temporalWorkflowId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> input;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> output;

    @Column(name = "current_node_id")
    private UUID currentNodeId;

    @Column(name = "started_by")
    private String startedBy;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @PrePersist
    protected void onPersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // JPA auto-calls this
    }
}
