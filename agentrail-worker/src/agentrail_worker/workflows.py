"""Temporal workflow – orchestrates graph execution node by node."""

from __future__ import annotations

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

# End sentinel
_END = "END"

# Max nodes to prevent infinite loops
_MAX_NODES = 100


@workflow.defn(name="AgentRailRunWorkflow")
class AgentRailWorkflow:
    """Long-running workflow that walks a GraphSpec node by node.

    Matches the Java service's Temporal workflow type "AgentRailRunWorkflow".
    Signals / queries can be added later for human-in-the-loop patterns.
    """

    @workflow.run
    async def run(self, run_id: str) -> str:
        """Execute the graph for *run_id* and return a JSON RunResult string.

        The Java service starts this workflow via Temporal Client and expects
        the return value to be a string (JSON-serialized RunResult).
        """
        # 1. Load manifest (queries Postgres via activity)
        manifest_raw: dict = await workflow.execute_activity(
            "load_manifest",
            run_id,
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=_DEFAULT_RETRY,
        )

        graph_data = manifest_raw.get("graph", {})
        graph = GraphSpec(
            nodes=graph_data.get("nodes", []),
            edges=graph_data.get("edges", []),
            entry_node=graph_data.get("entry_node", "start"),
        )
        entry_node = graph_data.get("entry_node", graph.entry_node)

        # 2. Walk the graph
        current_node_id: str | None = entry_node
        state: dict = manifest_raw.get("variables", {})
        steps = 0

        while current_node_id and current_node_id != _END and steps < _MAX_NODES:
            node = graph.get_node(current_node_id)
            if node is None:
                result = RunResult(
                    run_id=run_id,
                    status=RunStatus.FAILED,
                    final_state=state,
                    error=f"Node '{current_node_id}' not found in graph",
                )
                return result.model_dump_json()

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
                start_to_close_timeout=timedelta(minutes=10),
                retry_policy=_DEFAULT_RETRY,
            )
            output = NodeOutput.model_validate(output_raw)

            # Propagate state
            state = output.state

            # Check for errors
            if output.error:
                result = RunResult(
                    run_id=run_id,
                    status=RunStatus.FAILED,
                    final_state=state,
                    error=output.error,
                )
                return result.model_dump_json()

            # Determine next node
            if output.next_node_id:
                current_node_id = output.next_node_id
            else:
                successors = graph.next_nodes(current_node_id)
                current_node_id = successors[0] if successors else _END

            steps += 1

        # 3. Check for infinite loop guard
        if steps >= _MAX_NODES:
            result = RunResult(
                run_id=run_id,
                status=RunStatus.FAILED,
                final_state=state,
                error=f"Exceeded maximum node executions ({_MAX_NODES})",
            )
            return result.model_dump_json()

        # 4. Completed
        result = RunResult(
            run_id=run_id,
            status=RunStatus.COMPLETED,
            final_state=state,
        )
        return result.model_dump_json()
