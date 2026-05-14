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
@Table(name = "graph_specs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphSpec {

    @Id
    private UUID id;

    @Column(name = "entry_node_id")
    private String entryNodeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_schema", columnDefinition = "jsonb")
    private Map<String, Object> inputSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_schema", columnDefinition = "jsonb")
    private Map<String, Object> outputSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_schema", columnDefinition = "jsonb")
    private Map<String, Object> stateSchema;

    @PrePersist
    protected void onPersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
