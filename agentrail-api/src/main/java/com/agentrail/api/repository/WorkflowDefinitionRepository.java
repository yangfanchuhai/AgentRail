package com.agentrail.api.repository;

import com.agentrail.api.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    List<WorkflowDefinition> findByWorkspaceId(UUID workspaceId);

    Optional<WorkflowDefinition> findByWorkspaceIdAndKey(UUID workspaceId, String key);
}
