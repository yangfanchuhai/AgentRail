# ── Build stage ──
FROM python:3.12-slim AS build
WORKDIR /build

COPY pyproject.toml .
COPY src ./src

RUN pip install --no-cache-dir .

# ── Runtime stage ──
FROM python:3.12-slim
WORKDIR /app

RUN groupadd -r agentrail && useradd -r -g agentrail agentrail

COPY --from=build /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=build /usr/local/bin/agentrail-worker /usr/local/bin/agentrail-worker

USER agentrail
ENTRYPOINT ["agentrail-worker"]
