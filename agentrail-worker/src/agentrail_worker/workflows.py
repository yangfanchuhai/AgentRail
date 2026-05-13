"""Temporal workflow – orchestrates graph execution node by node."""

from __future__ import annotations

import asyncio
from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy

with workflow.unsafe.imports_passed_through():
    from .models import GraphSpec, NodeOutput, RunResult, RunStatus

# ---------------------------------------------------------------------------
# Retry policy shared across activities
# ---------------------------------------------------------------------------

_DEFAULT_RETRY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    backoff_coefficient=2.0,
)

# End sentinel – matches GraphSpec.end_node default
_END = "END"


@workflow.defn
class AgentRailWorkflow:
    """Long-running workflow that walks a GraphSpec node by node.

    Signals / queries can be added later for human-in-the-loop patterns.
    """

    @workflow.run
    async def run(self, run_id: str) -> dict:
        """Execute the graph for *run_id* and return a serialised RunResult."""
        # 1. Load manifest
        manifest_raw: dict = await workflow.execute_activity(
            "load_manifest",
            run_id,
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=_DEFAULT_RETRY,
        )

        graph = GraphSpec.model_validate(manifest_raw["graph"])
        entry_node = manifest_raw["graph"].get("entry_node", graph.entry_node)

        # 2. Walk the graph
        current_node_id: str | None = entry_node
        state: dict = manifest_raw.get("variables", {})

        while current_node_id and current_node_id != _END:
            node = graph.get_node(current_node_id)
            if node is None:
                return RunResult(
                    run_id=run_id,
                    status=RunStatus.FAILED,
                    final_state=state,
                    error=f"Node '{current_node_id}' not found in graph",
                ).model_dump(mode="json")

            # Build NodeInput payload
            node_input = {
                "run_id": run_id,
                "node": node.model_dump(mode="json"),
                "state": state,
            }

            # Execute the node activity
            output_raw: dict = await workflow.execute_activity(
                "execute_node",
                node_input,
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=_DEFAULT_RETRY,
            )
            output = NodeOutput.model_validate(output_raw)

            # Propagate state
            state = output.state

            # Check for errors
            if output.error:
                return RunResult(
                    run_id=run_id,
                    status=RunStatus.FAILED,
                    final_state=state,
                    error=output.error,
                ).model_dump(mode="json")

            # Determine next node
            if output.next_node_id:
                current_node_id = output.next_node_id
            else:
                successors = graph.next_nodes(current_node_id)
                current_node_id = successors[0] if successors else _END

        # 3. Completed
        return RunResult(
            run_id=run_id,
            status=RunStatus.COMPLETED,
            final_state=state,
        ).model_dump(mode="json")
