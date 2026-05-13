"""Temporal activities – each activity executes a single Agent node."""

from __future__ import annotations

import logging
from typing import Any

from temporalio import activity

from .config import settings
from .models import NodeInput, NodeOutput, NodeSpec, WorkflowManifest

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Manifest loading stub – will be backed by Postgres / MinIO
# ---------------------------------------------------------------------------


async def _load_manifest(run_id: str) -> WorkflowManifest:
    """Load the WorkflowManifest for *run_id* from persistent storage.

    TODO: implement with SQLAlchemy + asyncpg.
    For now returns a minimal placeholder so the worker can start.
    """
    logger.info("Loading manifest for run_id=%s (stub)", run_id)
    # Placeholder – in production this queries the database.
    from .models import GraphSpec, NodeSpec

    graph = GraphSpec(
        nodes=[NodeSpec(id="start", type="agent", label="Start")],
        entry_node="start",
    )
    return WorkflowManifest(run_id=run_id, graph=graph)


# ---------------------------------------------------------------------------
# Agent execution stub – will be backed by LangGraph
# ---------------------------------------------------------------------------


async def _execute_agent_node(node: NodeSpec, state: dict[str, Any]) -> dict[str, Any]:
    """Execute a single agent node via LangGraph.

    TODO: wire up LangGraph graph compilation and execution.
    """
    logger.info("Executing node id=%s type=%s (LangGraph stub)", node.id, node.type)
    # Placeholder: just echo state back with a timestamp.
    import datetime

    state["_last_node"] = node.id
    state["_last_executed_at"] = datetime.datetime.utcnow().isoformat()
    return state


# ---------------------------------------------------------------------------
# Temporal Activities
# ---------------------------------------------------------------------------


@activity.defn
async def load_manifest(run_id: str) -> dict[str, Any]:
    """Activity: load and return the serialized WorkflowManifest."""
    manifest = await _load_manifest(run_id)
    return manifest.model_dump(mode="json")


@activity.defn
async def execute_node(input_payload: dict[str, Any]) -> dict[str, Any]:
    """Activity: execute a single graph node and return the updated state.

    Parameters
    ----------
    input_payload:
        JSON-serialised :class:`NodeInput`.

    Returns
    -------
    dict
        JSON-serialised :class:`NodeOutput`.
    """
    node_input = NodeInput.model_validate(input_payload)
    try:
        updated_state = await _execute_agent_node(node_input.node, node_input.state)
        output = NodeOutput(
            run_id=node_input.run_id,
            node_id=node_input.node.id,
            result=None,
            state=updated_state,
        )
    except Exception as exc:
        logger.exception("Node execution failed: node_id=%s", node_input.node.id)
        output = NodeOutput(
            run_id=node_input.run_id,
            node_id=node_input.node.id,
            state=node_input.state,
            error=str(exc),
        )
    return output.model_dump(mode="json")
