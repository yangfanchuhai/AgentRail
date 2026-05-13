# agentrail-worker

Python 3.12 execution worker for AgentRail, powered by **Temporal** (workflow orchestration), **LangGraph** (agent execution), and **Pydantic v2** (data validation).

## Architecture

```
┌─────────────┐    ┌──────────────────┐    ┌──────────┐
│  API Server │───▶│  Temporal Server  │◀──▶│  Worker  │
└─────────────┘    └──────────────────┘    └────┬─────┘
                                                │
                                    ┌───────────┼───────────┐
                                    ▼           ▼           ▼
                              LangGraph    PostgreSQL     MinIO
                              (LLM call)   (state)       (artifacts)
```

## Quick Start

```bash
# Install uv (if not already)
curl -LsSf https://astral.sh/uv/install.sh | sh

# Create venv & install deps
uv sync

# Run worker (connects to Temporal at localhost:7233)
uv run agentrail-worker
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `TEMPORAL_HOST` | `localhost:7233` | Temporal server gRPC address |
| `TEMPORAL_NAMESPACE` | `default` | Temporal namespace |
| `TEMPORAL_TASK_QUEUE` | `agentrail` | Task queue name |
| `DATABASE_URL` | `postgresql+asyncpg://agentrail:agentrail@localhost:5432/agentrail` | Postgres connection string |
| `MINIO_ENDPOINT` | `localhost:9000` | MinIO / S3 endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `MINIO_BUCKET` | `agentrail` | MinIO bucket for artifacts |
| `OPENAI_API_KEY` | — | OpenAI API key (required for LLM nodes) |

## Project Structure

```
agentrail-worker/
├── src/agentrail_worker/
│   ├── __init__.py       # Package init
│   ├── worker.py         # Temporal worker entrypoint
│   ├── workflows.py      # Temporal workflow definitions
│   ├── activities.py     # Temporal activity definitions
│   ├── config.py         # Configuration management
│   └── models.py         # Pydantic v2 domain models
├── pyproject.toml
└── .python-version
```

## Development

```bash
# Lint
uv run ruff check .

# Format
uv run ruff format .
```
