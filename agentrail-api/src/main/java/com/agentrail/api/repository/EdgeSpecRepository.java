package com.agentrail.api.repository;

import com.agentrail.api.entity.EdgeSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EdgeSpecRepository extends JpaRepository<EdgeSpec, UUID> {

    List<EdgeSpec> findBySourceNodeId(UUID sourceNodeId);

    List<EdgeSpec> findBySourceNodeIdIn(List<UUID> sourceNodeIds);

    void deleteByGraphSpecId(UUID graphSpecId);
}
