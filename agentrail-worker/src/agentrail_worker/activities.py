"""Temporal activities – load manifest from DB, execute nodes, report status."""

from __future__ import annotations

import logging
from typing import Any

from temporalio import activity

from .callbacks import (
    create_step_run,
    update_step_run_status,
    update_current_node,
)
from .db import load_run_manifest
from .models import NodeInput, NodeOutput, NodeSpec

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Activity: load_manifest
# ---------------------------------------------------------------------------


@activity.defn
async def load_manifest(run_id: str) -> dict[str, Any]:
    """Load the full workflow manifest from PostgreSQL.

    Returns a dict with keys: graph (GraphSpec-compatible), variables, metadata.
    The graph is reconstructed from node_specs + edge_specs rows.
    """
    logger.info("Loading manifest for run_id=%s", run_id)
    data = await load_run_manifest(run_id)

    nodes_raw = data.get("nodes", [])
    edges_raw = data.get("edges", [])

    # Convert DB rows to GraphSpec-compatible format
    nodes = []
    for n in nodes_raw:
        node = {
            "id": str(n["id"]),
            "key": n.get("key", ""),
            "type": n.get("type", "agent"),
            "label": n.get("label", n.get("key", "")),
            "config": n.get("config", {}) or {},
        }
        # If node has an agent_version_id, embed it in config for executor
        if n.get("agent_version_id"):
            node["config"]["agent_version_id"] = str(n["agent_version_id"])
        if n.get("tool_version_id"):
            node["config"]["tool_version_id"] = str(n["tool_version_id"])
        nodes.append(node)

    edges = []
    for e in edges_raw:
        edges.append({
            "id": str(e["id"]),
            "source": str(e["source_node_id"]),
            "target": str(e["target_node_id"]),
            "condition": e.get("condition"),
        })

    # Determine entry_node from graph metadata or first node
    graph_data = data.get("graph", {})
    entry_node = graph_data.get("entry_node_key")
    if not entry_node and nodes:
        entry_node = nodes[0]["id"]

    manifest_data = data.get("manifest", {})
    variables = manifest_data.get("variables") or {}

    # Build the GraphSpec-compatible structure
    return {
        "graph": {
            "nodes": nodes,
            "edges": edges,
            "entry_node": entry_node or "start",
        },
        "variables": variables,
        "metadata": {
            "run_id": run_id,
            "workflow_version_id": str(data["run"]["workflow_version_id"]),
        },
    }


# ---------------------------------------------------------------------------
# Activity: execute_node
# ---------------------------------------------------------------------------


@activity.defn
async def execute_node(input_payload: dict[str, Any]) -> dict[str, Any]:
    """Execute a single graph node.

    Lifecycle:
    1. Create StepRun via callback
    2. Update StepRun → running
    3. Execute node logic (agent/tool/router)
    4. Update StepRun → completed/failed
    5. Return NodeOutput
    """
    node_input = NodeInput.model_validate(input_payload)
    run_id = node_input.run_id
    node = node_input.node
    state = node_input.state

    logger.info("Executing node id=%s type=%s label=%s", node.id, node.type, node.label)

    # 1. Create StepRun
    step_data = await create_step_run(
        run_id=run_id,
        node_spec_id=node.id,
        input_data={"state_keys": list(state.keys())},
    )
    step_run_id = step_data.get("id") if step_data else None

    # 2. Mark running
    if step_run_id:
        await update_step_run_status(run_id, step_run_id, "running")
        await update_current_node(run_id, node.id)

    # 3. Execute based on node type
    try:
        result_data = await _dispatch_node(node, state)
        output = NodeOutput(
            run_id=run_id,
            node_id=node.id,
            result=result_data.get("result"),
            state={**state, **result_data.get("state_update", {})},
            next_node_id=result_data.get("next_node_id"),
        )

        # 4. Mark completed
        if step_run_id:
            await update_step_run_status(
                run_id, step_run_id, "completed",
                output={"result": result_data.get("result")},
            )

    except Exception as exc:
        logger.exception("Node execution failed: node_id=%s", node.id)
        output = NodeOutput(
            run_id=run_id,
            node_id=node.id,
            state=state,
            error=str(exc),
        )
        # Mark failed
        if step_run_id:
            await update_step_run_status(
                run_id, step_run_id, "failed",
                error={"message": str(exc), "type": type(exc).__name__},
            )

    return output.model_dump(mode="json")


# ---------------------------------------------------------------------------
# Node type dispatch
# ---------------------------------------------------------------------------


async def _dispatch_node(node: NodeSpec, state: dict[str, Any]) -> dict[str, Any]:
    """Dispatch to the appropriate executor based on node type."""
    node_type = node.type

    if node_type == "agent":
        return await _execute_agent_node(node, state)
    elif node_type == "tool":
        return await _execute_tool_node(node, state)
    elif node_type == "router":
        return await _execute_router_node(node, state)
    elif node_type == "input":
        return {"state_update": {}, "result": "input_passthrough"}
    elif node_type == "output":
        return {"state_update": {}, "result": state}
    else:
        logger.warning("Unknown node type '%s', treating as passthrough", node_type)
        return {"state_update": {}, "result": None}


async def _execute_agent_node(node: NodeSpec, state: dict[str, Any]) -> dict[str, Any]:
    """Execute an agent node.

    Production: uses LangGraph with the agent_version referenced in node config.
    Current: stub that echoes state with metadata.
    """
    logger.info("Agent node id=%s executing (stub → will integrate LangGraph)", node.id)

    agent_version_id = node.config.get("agent_version_id")
    model_policy = node.config.get("model_policy_ref", "default")

    # TODO: Load agent_version from DB, build LangGraph, execute
    # For now: simulate agent execution
    import datetime

    state_update = {
        f"_agent_{node.id}_last_run": datetime.datetime.utcnow().isoformat(),
        f"_agent_{node.id}_status": "completed",
    }
    if agent_version_id:
        state_update[f"_agent_{node.id}_version"] = agent_version_id

    return {
        "state_update": state_update,
        "result": f"Agent {node.label} executed successfully",
    }


async def _execute_tool_node(node: NodeSpec, state: dict[str, Any]) -> dict[str, Any]:
    """Execute a tool node."""
    logger.info("Tool node id=%s executing (stub)", node.id)
    return {
        "state_update": {f"_tool_{node.id}_status": "completed"},
        "result": f"Tool {node.label} executed",
    }


async def _execute_router_node(node: NodeSpec, state: dict[str, Any]) -> dict[str, Any]:
    """Execute a router node – evaluates conditions and picks next node."""
    logger.info("Router node id=%s evaluating conditions", node.id)
    # Router logic: check condition in config, return next_node_id
    condition = node.config.get("default_route")
    return {
        "state_update": {f"_router_{node.id}_route": condition or "default"},
        "result": "routed",
        "next_node_id": condition,  # may be None → use graph edges
    }
