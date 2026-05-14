package com.agentrail.service.controller;

import com.agentrail.service.dto.*;
import com.agentrail.service.service.RunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal callback API — called by the Python worker
 * to update Run/StepRun status during execution.
 *
 * <p>These endpoints should be protected by an internal API key in production.</p>
 */
@RestController
@RequestMapping("/api/v1/internal/runs")
@RequiredArgsConstructor
public class RunCallbackController {

    private final RunService runService;

    /**
     * Update run status (e.g., running → completed, running → failed).
     */
    @PostMapping("/{runId}/status")
    public RunDto updateRunStatus(
            @PathVariable UUID runId,
            @RequestBody UpdateRunStatusRequest request) {
        return runService.updateRunStatus(runId, request.status(), request.output());
    }

    /**
     * Update current node (worker transitions to next node).
     */
    @PostMapping("/{runId}/current-node")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCurrentNode(
            @PathVariable UUID runId,
            @RequestBody UpdateCurrentNodeRequest request) {
        runService.updateRunCurrentNode(runId, request.nodeSpecId());
    }

    /**
     * Create a new StepRun when worker starts executing a node.
     */
    @PostMapping("/{runId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public StepRunDto createStepRun(
            @PathVariable UUID runId,
            @RequestBody CreateStepRunRequest request) {
        return runService.createStepRun(runId, request.nodeSpecId(), request.input());
    }

    /**
     * Update StepRun status (e.g., pending → running → completed/failed).
     */
    @PostMapping("/{runId}/steps/{stepRunId}/status")
    public StepRunDto updateStepRunStatus(
            @PathVariable UUID runId,
            @PathVariable UUID stepRunId,
            @RequestBody UpdateStepRunStatusRequest request) {
        return runService.updateStepRun(stepRunId, request.status(), request.output(), request.error());
    }

    // ── Internal request DTOs ──

    public record UpdateRunStatusRequest(String status, Map<String, Object> output) {}
    public record UpdateCurrentNodeRequest(UUID nodeSpecId) {}
    public record CreateStepRunRequest(UUID nodeSpecId, Map<String, Object> input) {}
    public record UpdateStepRunStatusRequest(String status, Map<String, Object> output, Map<String, Object> error) {}
}
