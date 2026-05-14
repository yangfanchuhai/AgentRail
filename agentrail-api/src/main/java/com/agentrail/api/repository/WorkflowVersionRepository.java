package com.agentrail.api.repository;

import com.agentrail.api.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {

    List<WorkflowVersion> findByWorkflowDefinitionIdOrderByCreatedAtDesc(UUID definitionId);

    Optional<WorkflowVersion> findByWorkflowDefinitionIdAndVersion(UUID definitionId, String version);

    boolean existsByWorkflowDefinitionIdAndStatus(UUID definitionId, String status);
}
