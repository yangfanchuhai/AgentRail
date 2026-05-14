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
@Table(name = "edge_specs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeSpec {

    @Id
    private UUID id;

    @Column(name = "source_node_id", nullable = false)
    private UUID sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private UUID targetNodeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> condition;

    @Builder.Default
    private Integer priority = 0;

    private String label;

    @PrePersist
    protected void onPersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
