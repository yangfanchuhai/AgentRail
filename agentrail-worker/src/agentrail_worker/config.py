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
    namespace: str = field(default_factory=lambda: _env("TEMPORAL_NAMESPACE", "default"))
    task_queue: str = field(default_factory=lambda: _env("TEMPORAL_TASK_QUEUE", "agentrail"))


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
class MinioConfig:
    """MinIO / S3-compatible object storage settings."""

    endpoint: str = field(default_factory=lambda: _env("MINIO_ENDPOINT", "localhost:9000"))
    access_key: str = field(default_factory=lambda: _env("MINIO_ACCESS_KEY", "minioadmin"))
    secret_key: str = field(default_factory=lambda: _env("MINIO_SECRET_KEY", "minioadmin"))
    bucket: str = field(default_factory=lambda: _env("MINIO_BUCKET", "agentrail"))
    secure: bool = field(default_factory=lambda: _env("MINIO_SECURE", "false").lower() == "true")


@dataclass(frozen=True)
class Settings:
    """Root settings object aggregating all config sections."""

    temporal: TemporalConfig = field(default_factory=TemporalConfig)
    postgres: PostgresConfig = field(default_factory=PostgresConfig)
    minio: MinioConfig = field(default_factory=MinioConfig)
    openai_api_key: str = field(default_factory=lambda: _env("OPENAI_API_KEY", ""))


# Module-level singleton – cheap to construct, safe to import everywhere.
settings = Settings()
