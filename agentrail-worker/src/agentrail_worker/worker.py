"""Temporal worker entrypoint – registers workflows & activities, then polls."""

from __future__ import annotations

import asyncio
import logging
import sys

from temporalio.client import Client
from temporalio.worker import Worker

from .activities import execute_node, load_manifest
from .config import settings
from .workflows import AgentRailWorkflow

logger = logging.getLogger("agentrail_worker")


async def _run() -> None:
    """Connect to Temporal and start the worker."""
    temporal = settings.temporal
    target = f"{temporal.host}"

    logger.info("Connecting to Temporal at %s (namespace=%s) …", target, temporal.namespace)
    client = await Client.connect(
        target_host=target,
        namespace=temporal.namespace,
    )

    worker = Worker(
        client=client,
        task_queue=temporal.task_queue,
        workflows=[AgentRailWorkflow],
        activities=[load_manifest, execute_node],
    )

    logger.info(
        "Worker started – task_queue=%s  workflows=[AgentRailWorkflow]  activities=[load_manifest, execute_node]",
        temporal.task_queue,
    )
    await worker.run()


def main() -> None:
    """CLI entrypoint registered via pyproject.toml [project.scripts]."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
        stream=sys.stderr,
    )
    try:
        asyncio.run(_run())
    except KeyboardInterrupt:
        logger.info("Worker shut down by user")


if __name__ == "__main__":
    main()
