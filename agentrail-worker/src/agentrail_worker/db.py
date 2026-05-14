"""Database access layer – async SQLAlchemy for reading workflow manifests."""

from __future__ import annotations

import logging
from typing import Any

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker

from .config import settings

logger = logging.getLogger(__name__)

_engine = create_async_engine(settings.postgres.url, echo=False, pool_size=10, max_overflow=20)
_session_factory = async_sessionmaker(_engine, class_=AsyncSession, expire_on_commit=False)


async def get_session() -> AsyncSession:
    """Get a new database session."""
    return _session_factory()


# ---------------------------------------------------------------------------
# Manifest loading
# ---------------------------------------------------------------------------


async def load_run_manifest(run_id: str) -> dict[str, Any]:
    """Load the full execution context for a run from Postgres.

    Returns a dict with keys: run, manifest, graph, nodes, edges.
    """
    async with await get_session() as session:
        # 1. Load Run
        run_row = await session.execute(
            text("SELECT * FROM runs WHERE id = :id"), {"id": run_id}
        )
        run = run_row.mappings().first()
        if not run:
            raise ValueError(f"Run not found: {run_id}")

        # 2. Load Manifest via workflow_version_id
        manifest_row = await session.execute(
            text("""
                SELECT m.*, v.workflow_definition_id
                FROM workflow_manifests m
                JOIN workflow_versions v ON v.id = m.workflow_version_id
                WHERE v.id = :version_id
            """),
            {"version_id": str(run["workflow_version_id"])},
        )
        manifest = manifest_row.mappings().first()
        if not manifest:
            raise ValueError(f"Manifest not found for workflow_version_id={run['workflow_version_id']}")

        # 3. Load Graph
        graph_row = await session.execute(
            text("SELECT * FROM graph_specs WHERE id = :id"),
            {"id": str(manifest["graph_spec_id"])},
        )
        graph = graph_row.mappings().first()

        # 4. Load Nodes
        nodes_result = await session.execute(
            text("SELECT * FROM node_specs WHERE graph_spec_id = :id ORDER BY key"),
            {"id": str(manifest["graph_spec_id"])},
        )
        nodes = [dict(row) for row in nodes_result.mappings().all()]

        # 5. Load Edges (by source node IDs)
        node_ids = [str(n["id"]) for n in nodes]
        edges = []
        if node_ids:
            edges_result = await session.execute(
                text("SELECT * FROM edge_specs WHERE source_node_id = ANY(:ids)"),
                {"ids": node_ids},
            )
            edges = [dict(row) for row in edges_result.mappings().all()]

        return {
            "run": dict(run),
            "manifest": dict(manifest),
            "graph": dict(graph) if graph else {},
            "nodes": nodes,
            "edges": edges,
        }


# ---------------------------------------------------------------------------
# Agent version loading (for executor_ref → agent binding)
# ---------------------------------------------------------------------------


async def load_agent_version(agent_version_id: str) -> dict[str, Any]:
    """Load an agent version by ID."""
    async with await get_session() as session:
        result = await session.execute(
            text("SELECT * FROM agent_versions WHERE id = :id"),
            {"id": agent_version_id},
        )
        row = result.mappings().first()
        return dict(row) if row else {}
