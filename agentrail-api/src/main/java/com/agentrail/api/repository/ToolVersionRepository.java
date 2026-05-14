package com.agentrail.api.repository;

import com.agentrail.api.entity.ToolVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolVersionRepository extends JpaRepository<ToolVersion, UUID> {

    List<ToolVersion> findByToolDefinitionIdOrderByCreatedAtDesc(UUID definitionId);

    Optional<ToolVersion> findByToolDefinitionIdAndVersion(UUID definitionId, String version);
}
