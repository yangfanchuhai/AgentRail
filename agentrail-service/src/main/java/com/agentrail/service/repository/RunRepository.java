package com.agentrail.service.repository;

import com.agentrail.service.entity.Run;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RunRepository extends JpaRepository<Run, UUID> {

    List<Run> findByWorkflowVersionIdOrderByCreatedAtDesc(UUID workflowVersionId);

    List<Run> findByStatus(String status);

    List<Run> findByStartedByOrderByCreatedAtDesc(String startedBy);
}
