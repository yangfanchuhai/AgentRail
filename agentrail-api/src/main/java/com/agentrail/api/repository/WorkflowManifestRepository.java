package com.agentrail.api.repository;

import com.agentrail.api.entity.WorkflowManifest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowManifestRepository extends JpaRepository<WorkflowManifest, UUID> {

    Optional<WorkflowManifest> findByWorkflowVersionId(UUID workflowVersionId);
}
