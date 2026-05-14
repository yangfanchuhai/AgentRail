package com.agentrail.api.repository;

import com.agentrail.api.entity.AgentDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentDefinitionRepository extends JpaRepository<AgentDefinition, UUID> {

    List<AgentDefinition> findByWorkspaceId(UUID workspaceId);

    Optional<AgentDefinition> findByWorkspaceIdAndKey(UUID workspaceId, String key);
}
