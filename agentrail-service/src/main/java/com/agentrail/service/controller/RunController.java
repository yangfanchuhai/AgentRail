package com.agentrail.service.controller;

import com.agentrail.service.dto.*;
import com.agentrail.service.service.RunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public Run API — start, query, cancel workflow runs.
 */
@RestController
@RequestMapping("/api/v1/runs")
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunDto startRun(
            @Valid @RequestBody StartRunRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String startedBy) {
        return runService.startRun(request, startedBy);
    }

    @GetMapping("/{runId}")
    public RunDto getRun(@PathVariable UUID runId) {
        return runService.getRun(runId);
    }

    @GetMapping
    public List<RunDto> listRuns(
            @RequestParam(value = "workflowVersionId", required = false) UUID workflowVersionId,
            @RequestParam(value = "status", required = false) String status) {
        if (workflowVersionId != null) {
            return runService.listRunsByWorkflowVersion(workflowVersionId);
        }
        if (status != null) {
            return runService.listRunsByStatus(status);
        }
        return runService.listRunsByStatus("running"); // default: show active
    }

    @PostMapping("/{runId}/cancel")
    public RunDto cancelRun(@PathVariable UUID runId) {
        return runService.cancelRun(runId);
    }

    // ── StepRun queries ──

    @GetMapping("/{runId}/steps")
    public List<StepRunDto> listStepRuns(@PathVariable UUID runId) {
        return runService.listStepRuns(runId);
    }

    @GetMapping("/{runId}/steps/{stepRunId}")
    public StepRunDto getStepRun(@PathVariable UUID runId, @PathVariable UUID stepRunId) {
        return runService.getStepRun(runId, stepRunId);
    }
}
