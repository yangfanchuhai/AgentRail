package com.agentrail.service.service;

import com.agentrail.service.dto.*;
import com.agentrail.service.entity.Run;
import com.agentrail.service.entity.StepRun;
import com.agentrail.service.repository.RunRepository;
import com.agentrail.service.repository.StepRunRepository;
import com.agentrail.service.temporal.AgentRailRunWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunService {

    private final RunRepository runRepo;
    private final StepRunRepository stepRunRepo;
    private final WorkflowClient workflowClient;

    @Value("${temporal.worker.task-queue:agentrail-runs}")
    private String taskQueue;

    // ── Start Run ──

    @Transactional
    public RunDto startRun(StartRunRequest request, String startedBy) {
        UUID runId = UUID.randomUUID();

        // 1. Create Run record
        Run run = Run.builder()
                .id(runId)
                .workflowVersionId(request.workflowVersionId())
                .input(request.input())
                .startedBy(startedBy)
                .status("pending")
                .build();
        run = runRepo.save(run);

        // 2. Start Temporal workflow (async)
        try {
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId("agentrail-run-" + runId)
                    .setTaskQueue(taskQueue)
                    .build();

            AgentRailRunWorkflow workflow = workflowClient.newWorkflowStub(
                    AgentRailRunWorkflow.class, options);

            // Start async — non-blocking
            WorkflowExecution execution = WorkflowClient.start(workflow::execute, runId.toString());

            // 3. Update Run with temporal workflow ID
            run.setTemporalWorkflowId(execution.getWorkflowId());
            run.setStatus("running");
            run.setStartedAt(OffsetDateTime.now());
            run = runRepo.save(run);

            log.info("Started run {} with Temporal workflow {}", runId, execution.getWorkflowId());
        } catch (Exception e) {
            run.setStatus("failed");
            run.setEndedAt(OffsetDateTime.now());
            runRepo.save(run);
            log.error("Failed to start Temporal workflow for run {}: {}", runId, e.getMessage());
            throw new IllegalStateException("Failed to start workflow: " + e.getMessage(), e);
        }

        return toRunDto(run);
    }

    // ── Query Runs ──

    public RunDto getRun(UUID runId) {
        Run run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        return toRunDto(run);
    }

    public List<RunDto> listRunsByWorkflowVersion(UUID workflowVersionId) {
        return runRepo.findByWorkflowVersionIdOrderByCreatedAtDesc(workflowVersionId).stream()
                .map(this::toRunDto)
                .toList();
    }

    public List<RunDto> listRunsByStatus(String status) {
        return runRepo.findByStatus(status).stream()
                .map(this::toRunDto)
                .toList();
    }

    // ── StepRun Query ──

    public List<StepRunDto> listStepRuns(UUID runId) {
        return stepRunRepo.findByRunIdOrderByCreatedAtAsc(runId).stream()
                .map(this::toStepRunDto)
                .toList();
    }

    public StepRunDto getStepRun(UUID runId, UUID stepRunId) {
        StepRun step = stepRunRepo.findById(stepRunId)
                .orElseThrow(() -> new IllegalArgumentException("StepRun not found: " + stepRunId));
        if (!step.getRunId().equals(runId)) {
            throw new IllegalArgumentException("StepRun does not belong to run: " + runId);
        }
        return toStepRunDto(step);
    }

    // ── Cancel Run ──

    @Transactional
    public RunDto cancelRun(UUID runId) {
        Run run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));

        if ("completed".equals(run.getStatus()) || "failed".equals(run.getStatus())
                || "canceled".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot cancel run in status: " + run.getStatus());
        }

        // Cancel Temporal workflow
        if (run.getTemporalWorkflowId() != null) {
            try {
                AgentRailRunWorkflow stub = workflowClient.newWorkflowStub(
                        AgentRailRunWorkflow.class, run.getTemporalWorkflowId());
                workflowClient.newUntypedWorkflowStub(run.getTemporalWorkflowId()).cancel();
                log.info("Cancelled Temporal workflow {}", run.getTemporalWorkflowId());
            } catch (Exception e) {
                log.warn("Failed to cancel Temporal workflow {}: {}",
                        run.getTemporalWorkflowId(), e.getMessage());
            }
        }

        run.setStatus("canceled");
        run.setEndedAt(OffsetDateTime.now());
        run = runRepo.save(run);
        return toRunDto(run);
    }

    // ── Internal: update run status (called by callbacks from worker) ──

    @Transactional
    public RunDto updateRunStatus(UUID runId, String status, Map<String, Object> output) {
        Run run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.setStatus(status);
        if (output != null) {
            run.setOutput(output);
        }
        if ("completed".equals(status) || "failed".equals(status) || "canceled".equals(status)) {
            run.setEndedAt(OffsetDateTime.now());
        }
        run = runRepo.save(run);
        return toRunDto(run);
    }

    @Transactional
    public void updateRunCurrentNode(UUID runId, UUID currentNodeId) {
        Run run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.setCurrentNodeId(currentNodeId);
        runRepo.save(run);
    }

    // ── StepRun create/update (called by worker) ──

    @Transactional
    public StepRunDto createStepRun(UUID runId, UUID nodeSpecId, Map<String, Object> input) {
        StepRun step = StepRun.builder()
                .runId(runId)
                .nodeSpecId(nodeSpecId)
                .input(input)
                .status("pending")
                .build();
        step = stepRunRepo.save(step);
        return toStepRunDto(step);
    }

    @Transactional
    public StepRunDto updateStepRun(UUID stepRunId, String status,
                                     Map<String, Object> output, Map<String, Object> error) {
        StepRun step = stepRunRepo.findById(stepRunId)
                .orElseThrow(() -> new IllegalArgumentException("StepRun not found: " + stepRunId));
        step.setStatus(status);
        if (output != null) {
            step.setOutput(output);
        }
        if (error != null) {
            step.setError(error);
        }
        if ("running".equals(status) && step.getStartedAt() == null) {
            step.setStartedAt(OffsetDateTime.now());
        }
        if ("completed".equals(status) || "failed".equals(status)
                || "skipped".equals(status) || "canceled".equals(status)) {
            step.setEndedAt(OffsetDateTime.now());
        }
        step = stepRunRepo.save(step);
        return toStepRunDto(step);
    }

    // ── Mappers ──

    private RunDto toRunDto(Run r) {
        return new RunDto(r.getId(), r.getWorkflowVersionId(), r.getTemporalWorkflowId(),
                r.getStatus(), r.getInput(), r.getOutput(), r.getCurrentNodeId(),
                r.getStartedBy(), r.getStartedAt(), r.getEndedAt());
    }

    private StepRunDto toStepRunDto(StepRun s) {
        return new StepRunDto(s.getId(), s.getRunId(), s.getNodeSpecId(), s.getStatus(),
                s.getAttempt(), s.getInput(), s.getOutput(), s.getError(),
                s.getStartedAt(), s.getEndedAt());
    }
}
