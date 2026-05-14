package com.agentrail.service.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal Workflow interface for executing an AgentRail run.
 *
 * <p>The Java service only starts this workflow via Temporal Client.
 * The actual workflow implementation is registered by the Python worker
 * using the same workflow type string.</p>
 *
 * <p>Workflow type: "AgentRailRunWorkflow"</p>
 */
@WorkflowInterface
public interface AgentRailRunWorkflow {

    /**
     * Execute a workflow run.
     *
     * @param runId the Run entity UUID
     * @return the final output of the run
     */
    @WorkflowMethod(name = "AgentRailRunWorkflow")
    String execute(String runId);
}
