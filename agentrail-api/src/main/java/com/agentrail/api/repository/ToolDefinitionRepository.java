package com.agentrail.api.repository;

import com.agentrail.api.entity.ToolDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolDefinitionRepository extends JpaRepository<ToolDefinition, UUID> {

    List<ToolDefinition> findByWorkspaceId(UUID workspaceId);

    Optional<ToolDefinition> findByWorkspaceIdAndKey(UUID workspaceId, String key);
}
