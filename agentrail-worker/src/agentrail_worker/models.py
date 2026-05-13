"""Core domain models for AgentRail – Pydantic v2."""

from __future__ import annotations

from enum import Enum
from typing import Any, Literal

from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Graph definition models
# ---------------------------------------------------------------------------


class NodeSpec(BaseModel):
    """A single node (agent step) in the workflow graph."""

    id: str = Field(..., description="Unique node identifier within the graph")
    type: str = Field(
        default="agent",
        description="Node type – 'agent', 'tool', 'router', 'input', 'output', etc.",
    )
    label: str = Field(default="", description="Human-readable label")
    config: dict[str, Any] = Field(
        default_factory=dict,
        description="Node-level configuration (model, prompt template, tools, …)",
    )


class EdgeSpec(BaseModel):
    """A directed edge connecting two nodes in the workflow graph."""

    id: str = Field(..., description="Unique edge identifier")
    source: str = Field(..., description="Source node id")
    target: str = Field(..., description="Target node id")
    condition: str | None = Field(
        default=None,
        description="Optional condition expression for conditional edges",
    )


class GraphSpec(BaseModel):
    """Complete graph definition – the executable blueprint for a workflow run."""

    nodes: list[NodeSpec] = Field(default_factory=list)
    edges: list[EdgeSpec] = Field(default_factory=list)
    entry_node: str = Field(..., description="id of the entry node")
    end_node: str = Field(default="END", description="id of the terminal node")

    # -- helpers ----------------------------------------------------------

    def get_node(self, node_id: str) -> NodeSpec | None:
        """Look up a node by id."""
        return next((n for n in self.nodes if n.id == node_id), None)

    def next_nodes(self, current: str) -> list[str]:
        """Return target node ids reachable from *current* (ignoring conditions)."""
        return [e.target for e in self.edges if e.source == current]


# ---------------------------------------------------------------------------
# Workflow manifest (loaded by activities at runtime)
# ---------------------------------------------------------------------------


class WorkflowManifest(BaseModel):
    """Full manifest describing a workflow run – persisted in Postgres / MinIO."""

    run_id: str
    graph: GraphSpec
    variables: dict[str, Any] = Field(default_factory=dict, description="Run-level variables")
    metadata: dict[str, Any] = Field(default_factory=dict)


# ---------------------------------------------------------------------------
# Activity-level I/O
# ---------------------------------------------------------------------------


class NodeInput(BaseModel):
    """Input payload passed to a single node execution."""

    run_id: str
    node: NodeSpec
    state: dict[str, Any] = Field(default_factory=dict, description="Accumulated workflow state")


class NodeOutput(BaseModel):
    """Output produced by a single node execution."""

    run_id: str
    node_id: str
    result: Any = Field(default=None, description="Node execution result")
    state: dict[str, Any] = Field(default_factory=dict, description="Updated workflow state")
    next_node_id: str | None = Field(
        default=None,
        description="Override the next node id (for conditional routing)",
    )
    error: str | None = Field(default=None)


# ---------------------------------------------------------------------------
# Workflow-level result
# ---------------------------------------------------------------------------


class RunStatus(str, Enum):
    """Terminal status of a workflow run."""

    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class RunResult(BaseModel):
    """Final result returned by the Temporal workflow."""

    run_id: str
    status: RunStatus
    final_state: dict[str, Any] = Field(default_factory=dict)
    error: str | None = Field(default=None)
