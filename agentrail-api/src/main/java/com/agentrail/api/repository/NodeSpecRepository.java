package com.agentrail.api.repository;

import com.agentrail.api.entity.NodeSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NodeSpecRepository extends JpaRepository<NodeSpec, UUID> {

    List<NodeSpec> findByGraphSpecId(UUID graphSpecId);

    Optional<NodeSpec> findByGraphSpecIdAndKey(UUID graphSpecId, String key);
}
