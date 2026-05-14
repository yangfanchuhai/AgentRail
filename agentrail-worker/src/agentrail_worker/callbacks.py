"""Callback client – reports run/step status back to agentrail-service."""

from __future__ import annotations

import logging
from typing import Any
from uuid import UUID

import httpx

from .config import settings

logger = logging.getLogger(__name__)

_base = settings.service.base_url.rstrip("/")
_client = httpx.AsyncClient(timeout=30.0)


async def _post(path: str, payload: dict) -> dict | None:
    """POST JSON to the service and return the response (or None on error)."""
    try:
        resp = await _client.post(f"{_base}{path}", json=payload)
        resp.raise_for_status()
        return resp.json()
    except Exception:
        logger.exception("Callback POST failed: %s", path)
        return None


# ---------------------------------------------------------------------------
# Run-level callbacks
# ---------------------------------------------------------------------------


async def update_run_status(run_id: str, status: str, output: dict | None = None) -> None:
    """Update the overall run status."""
    payload: dict[str, Any] = {"status": status}
    if output is not None:
        payload["output"] = output
    await _post(f"/api/v1/internal/runs/{run_id}/status", payload)


async def update_current_node(run_id: str, node_spec_id: str) -> None:
    """Update which node the run is currently executing."""
    await _post(f"/api/v1/internal/runs/{run_id}/current-node", {"nodeSpecId": node_spec_id})


# ---------------------------------------------------------------------------
# StepRun-level callbacks
# ---------------------------------------------------------------------------


async def create_step_run(run_id: str, node_spec_id: str, input_data: dict | None = None) -> dict | None:
    """Create a new StepRun and return its data."""
    payload: dict[str, Any] = {"nodeSpecId": node_spec_id}
    if input_data is not None:
        payload["input"] = input_data
    return await _post(f"/api/v1/internal/runs/{run_id}/steps", payload)


async def update_step_run_status(
    run_id: str, step_run_id: str, status: str,
    output: dict | None = None, error: dict | None = None,
) -> dict | None:
    """Update a StepRun's status."""
    payload: dict[str, Any] = {"status": status}
    if output is not None:
        payload["output"] = output
    if error is not None:
        payload["error"] = error
    return await _post(f"/api/v1/internal/runs/{run_id}/steps/{step_run_id}/status", payload)
