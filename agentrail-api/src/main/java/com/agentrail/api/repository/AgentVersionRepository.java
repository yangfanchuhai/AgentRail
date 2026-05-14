package com.agentrail.api.repository;

import com.agentrail.api.entity.AgentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentVersionRepository extends JpaRepository<AgentVersion, UUID> {

    List<AgentVersion> findByAgentDefinitionIdOrderByCreatedAtDesc(UUID definitionId);

    Optional<AgentVersion> findByAgentDefinitionIdAndVersion(UUID definitionId, String version);
}
