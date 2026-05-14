package com.agentrail.service.repository;

import com.agentrail.service.entity.StepRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StepRunRepository extends JpaRepository<StepRun, UUID> {

    List<StepRun> findByRunIdOrderByCreatedAtAsc(UUID runId);

    List<StepRun> findByRunIdAndStatus(UUID runId, String status);
}
