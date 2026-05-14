package com.agentrail.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "node_specs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeSpec {

    @Id
    private UUID id;

    @Column(name = "graph_spec_id", nullable = false)
    private UUID graphSpecId;

    @Column(nullable = false)
    private String key;

    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String type = "agent";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "executor_ref", columnDefinition = "jsonb")
    private Map<String, Object> executorRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> config = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_mapping", columnDefinition = "jsonb")
    private Map<String, Object> inputMapping;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_mapping", columnDefinition = "jsonb")
    private Map<String, Object> outputMapping;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checkpoint_policy", columnDefinition = "jsonb")
    private Map<String, Object> checkpointPolicy;

    @Column(name = "transition_strategy", length = 50)
    private String transitionStrategy;

    @PrePersist
    protected void onPersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
