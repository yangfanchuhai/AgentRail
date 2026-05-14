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
@Table(name = "step_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepRun {

    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "node_spec_id", nullable = false)
    private UUID nodeSpecId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(nullable = false)
    @Builder.Default
    private Integer attempt = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> input;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> output;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> error;

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
}
