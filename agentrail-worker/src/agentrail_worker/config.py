"""AgentRail Worker configuration – loaded from environment variables."""

from __future__ import annotations

import os
from dataclasses import dataclass, field


def _env(key: str, default: str) -> str:
    return os.getenv(key, default)


@dataclass(frozen=True)
class TemporalConfig:
    """Temporal server connection settings."""

    host: str = field(default_factory=lambda: _env("TEMPORAL_HOST", "localhost:7233"))
    namespace: str = field(default_factory=lambda: _env("TEMPORAL_NAMESPACE", "agentrail"))
    task_queue: str = field(default_factory=lambda: _env("TEMPORAL_TASK_QUEUE", "agentrail-runs"))


@dataclass(frozen=True)
class PostgresConfig:
    """PostgreSQL connection settings."""

    url: str = field(
        default_factory=lambda: _env(
            "DATABASE_URL",
            "postgresql+asyncpg://agentrail:agentrail@localhost:5432/agentrail",
        )
    )


@dataclass(frozen=True)
class ServiceConfig:
    """AgentRail Service (Java data plane) callback URL."""

    base_url: str = field(
        default_factory=lambda: _env("AGENTRAIL_SERVICE_URL", "http://localhost:8082")
    )


@dataclass(frozen=True)
class Settings:
    """Root settings object aggregating all config sections."""

    temporal: TemporalConfig = field(default_factory=TemporalConfig)
    postgres: PostgresConfig = field(default_factory=PostgresConfig)
    service: ServiceConfig = field(default_factory=ServiceConfig)
    openai_api_key: str = field(default_factory=lambda: _env("OPENAI_API_KEY", ""))


# Module-level singleton
settings = Settings()
