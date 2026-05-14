package com.agentrail.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersion {

    @Id
    private UUID id;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    @Builder.Default
    private String status = "draft";

    @Column(name = "manifest_id")
    private UUID manifestId;

    @Column(length = 64)
    private String checksum;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "published_by")
    private String publishedBy;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

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
