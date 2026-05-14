package com.agentrail.api.repository;

import com.agentrail.api.entity.GraphSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GraphSpecRepository extends JpaRepository<GraphSpec, UUID> {
}
