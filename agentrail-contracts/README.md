# agentrail-contracts

Shared JSON Schema contracts between the **Java control/data plane** (`agentrail-api`, `agentrail-service`) and the **Python execution plane** (`agentrail-worker`).

## Purpose

These schemas define the canonical wire format for all domain objects exchanged via:
- Temporal workflow/activity payloads (JSON-serialised)
- REST API request/response bodies
- Database JSON columns
- Event / message payloads

Both Java (Jackson) and Python (Pydantic v2) implementations **must** produce and consume data conforming to these schemas. Any consumer can validate payloads against these schemas at runtime or build time.

## Schema Index

| Schema | Description |
|--------|-------------|
| [`nodespec.schema.json`](schemas/nodespec.schema.json) | A single node (agent step) in the workflow graph |
| [`edgespec.schema.json`](schemas/edgespec.schema.json) | A directed edge connecting two nodes |
| [`graphspec.schema.json`](schemas/graphspec.schema.json) | Complete workflow graph definition |
| [`workflow-manifest.schema.json`](schemas/workflow-manifest.schema.json) | Full manifest for a workflow run |
| [`agent-version.schema.json`](schemas/agent-version.schema.json) | Agent capability version registration |
| [`tool-version.schema.json`](schemas/tool-version.schema.json) | Tool definition version |
| [`run.schema.json`](schemas/run.schema.json) | Workflow run lifecycle (input, state, result) |

## Usage

### Java (Jackson)
```java
ObjectMapper mapper = new ObjectMapper();
JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
JsonSchema schema = factory.getSchema(
    getClass().getResourceAsStream("/schemas/graphspec.schema.json")
);
ValidationReport report = factory.validate(schema, mapper.valueToTree(graphSpec));
```

### Python (jsonschema / Pydantic)
```python
import jsonschema
from agentrail_worker.models import GraphSpec

# Validate raw dict before parsing
jsonschema.validate(payload, schema_resolved)
graph = GraphSpec.model_validate(payload)
```

## Versioning

Schemas follow **semantic versioning** within `$id`. Breaking changes (field removal, type change, new required field) bump the **major** version. Additive changes bump **minor**.

All schemas share the base URI: `https://agentrail.dev/schemas/<version>/`

## Reference

Field definitions are derived from the AgentRail design document §2 (Domain Objects). The authoritative source of truth is the JSON Schema files in this directory — **not** any single language implementation.
