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
@Table(name = "tool_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolVersion {

    @Id
    private UUID id;

    @Column(name = "tool_definition_id", nullable = false)
    private UUID toolDefinitionId;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    @Builder.Default
    private String type = "function";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_schema", columnDefinition = "jsonb")
    private Map<String, Object> inputSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_schema", columnDefinition = "jsonb")
    private Map<String, Object> outputSchema;

    @Column(name = "connection_ref")
    private String connectionRef;

    @Column(name = "mcp_server_ref")
    private String mcpServerRef;

    @Column(name = "mcp_tool_name")
    private String mcpToolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permission_scope", columnDefinition = "jsonb")
    private List<String> permissionScope;

    @Column(name = "side_effect_level", nullable = false)
    @Builder.Default
    private String sideEffectLevel = "none";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "idempotency_policy", columnDefinition = "jsonb")
    private Map<String, Object> idempotencyPolicy;

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
