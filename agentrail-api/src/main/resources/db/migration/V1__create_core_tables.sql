-- ============================================================
-- AgentRail — Flyway V1: Create core tables
-- First batch of core database tables per design doc §2 & §5
-- ============================================================

-- ============================================================
-- 1. tenants
-- ============================================================
CREATE TABLE tenants (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'created'
                CHECK (status IN ('created', 'active', 'suspended', 'archived')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- 2. workspaces
-- ============================================================
CREATE TABLE workspaces (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'created'
                CHECK (status IN ('created', 'active', 'suspended', 'archived')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_workspaces_tenant_slug UNIQUE (tenant_id, slug)
);

-- ============================================================
-- 3. workflow_definitions
-- ============================================================
CREATE TABLE workflow_definitions (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id      UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    key               VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    owner             VARCHAR(255),
    tags              JSONB       DEFAULT '[]',
    lifecycle_status  VARCHAR(20)  NOT NULL DEFAULT 'draft'
                      CHECK (lifecycle_status IN ('draft', 'active', 'deprecated', 'archived')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_wf_def_ws_key UNIQUE (workspace_id, key)
);

-- ============================================================
-- 4. workflow_versions
-- ============================================================
CREATE TABLE workflow_versions (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_definition_id  UUID        NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    version                 VARCHAR(50)  NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'draft'
                            CHECK (status IN ('draft', 'published', 'deprecated', 'revoked')),
    manifest_id             UUID,
    checksum                VARCHAR(64),
    change_summary          TEXT,
    published_by            VARCHAR(255),
    published_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_wf_ver_def_version UNIQUE (workflow_definition_id, version)
);

-- ============================================================
-- 6. graph_specs  (created before workflow_manifests / node_specs)
-- ============================================================
CREATE TABLE graph_specs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_node_id   VARCHAR(255),
    input_schema    JSONB,
    output_schema   JSONB,
    state_schema    JSONB
);

-- ============================================================
-- 7. node_specs
-- ============================================================
CREATE TABLE node_specs (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    graph_spec_id         UUID         NOT NULL REFERENCES graph_specs(id) ON DELETE CASCADE,
    key                   VARCHAR(255) NOT NULL,
    name                  VARCHAR(255),
    type                  VARCHAR(30)  NOT NULL DEFAULT 'agent'
                          CHECK (type IN ('agent', 'tool', 'router', 'input', 'output',
                                          'human', 'subgraph', 'parallel', 'delay')),
    executor_ref          JSONB,
    config                JSONB        DEFAULT '{}',
    input_mapping         JSONB,
    output_mapping        JSONB,
    checkpoint_policy     JSONB,
    transition_strategy   VARCHAR(50),
    CONSTRAINT uq_node_spec_gs_key UNIQUE (graph_spec_id, key)
);

-- ============================================================
-- 8. edge_specs
-- ============================================================
CREATE TABLE edge_specs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_node_id  UUID         NOT NULL REFERENCES node_specs(id) ON DELETE CASCADE,
    target_node_id  UUID         NOT NULL REFERENCES node_specs(id) ON DELETE CASCADE,
    condition       JSONB,
    priority        INT          NOT NULL DEFAULT 0,
    label           VARCHAR(255)
);

-- ============================================================
-- 5. workflow_manifests  (references graph_specs)
-- ============================================================
CREATE TABLE workflow_manifests (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id UUID        NOT NULL REFERENCES workflow_versions(id) ON DELETE CASCADE,
    graph_spec_id       UUID        NOT NULL REFERENCES graph_specs(id) ON DELETE CASCADE,
    agent_bindings      JSONB       DEFAULT '[]',
    skill_bindings      JSONB       DEFAULT '[]',
    tool_bindings       JSONB       DEFAULT '[]',
    model_bindings      JSONB       DEFAULT '[]',
    prompt_bindings     JSONB       DEFAULT '[]',
    policy_bindings    JSONB       DEFAULT '[]',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- back-fill manifest_id FK on workflow_versions
ALTER TABLE workflow_versions
    ADD CONSTRAINT fk_wf_ver_manifest
    FOREIGN KEY (manifest_id) REFERENCES workflow_manifests(id) ON DELETE SET NULL;

-- ============================================================
-- 9. agent_definitions
-- ============================================================
CREATE TABLE agent_definitions (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id      UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    key               VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    lifecycle_status  VARCHAR(20)  NOT NULL DEFAULT 'draft'
                      CHECK (lifecycle_status IN ('draft', 'active', 'deprecated', 'archived')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_agent_def_ws_key UNIQUE (workspace_id, key)
);

-- ============================================================
-- 10. agent_versions
-- ============================================================
CREATE TABLE agent_versions (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_definition_id   UUID         NOT NULL REFERENCES agent_definitions(id) ON DELETE CASCADE,
    version               VARCHAR(50)  NOT NULL,
    role                  VARCHAR(255),
    goal                  TEXT,
    system_instruction    TEXT,
    prompt_refs           JSONB        DEFAULT '[]',
    skill_refs            JSONB        DEFAULT '[]',
    tool_refs             JSONB        DEFAULT '[]',
    model_policy_ref      VARCHAR(255),
    planning_mode         VARCHAR(50),
    max_iterations        INT,
    stop_condition        JSONB,
    output_schema         JSONB,
    status                VARCHAR(20)  NOT NULL DEFAULT 'draft'
                          CHECK (status IN ('draft', 'published', 'deprecated', 'revoked')),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_agent_ver_def_version UNIQUE (agent_definition_id, version)
);

-- ============================================================
-- 11. tool_definitions
-- ============================================================
CREATE TABLE tool_definitions (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id      UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    key               VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    lifecycle_status  VARCHAR(20)  NOT NULL DEFAULT 'draft'
                      CHECK (lifecycle_status IN ('draft', 'active', 'deprecated', 'archived')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_tool_def_ws_key UNIQUE (workspace_id, key)
);

-- ============================================================
-- 12. tool_versions
-- ============================================================
CREATE TABLE tool_versions (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_definition_id    UUID         NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
    version               VARCHAR(50)  NOT NULL,
    type                  VARCHAR(20)  NOT NULL DEFAULT 'function'
                          CHECK (type IN ('function', 'http', 'mcp', 'internal', 'database')),
    input_schema          JSONB,
    output_schema         JSONB,
    connection_ref        VARCHAR(255),
    mcp_server_ref        VARCHAR(255),
    mcp_tool_name         VARCHAR(255),
    permission_scope      JSONB,
    side_effect_level     VARCHAR(20)  NOT NULL DEFAULT 'none'
                          CHECK (side_effect_level IN ('none', 'read', 'write', 'destructive', 'external_commit')),
    idempotency_policy    JSONB,
    status                VARCHAR(20)  NOT NULL DEFAULT 'draft'
                          CHECK (status IN ('draft', 'published', 'deprecated', 'revoked')),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_tool_ver_def_version UNIQUE (tool_definition_id, version)
);

-- ============================================================
-- 13. runs
-- ============================================================
CREATE TABLE runs (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id   UUID        NOT NULL REFERENCES workflow_versions(id) ON DELETE CASCADE,
    temporal_workflow_id  VARCHAR(255),
    status                VARCHAR(20)  NOT NULL DEFAULT 'pending'
                          CHECK (status IN ('pending', 'running', 'waiting', 'suspended',
                                            'completed', 'failed', 'canceled')),
    input                 JSONB,
    output                JSONB,
    current_node_id       UUID        REFERENCES node_specs(id) ON DELETE SET NULL,
    started_by            VARCHAR(255),
    started_at            TIMESTAMPTZ,
    ended_at              TIMESTAMPTZ
);

-- ============================================================
-- 14. step_runs
-- ============================================================
CREATE TABLE step_runs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id        UUID        NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    node_spec_id  UUID        NOT NULL REFERENCES node_specs(id) ON DELETE CASCADE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'pending'
                  CHECK (status IN ('pending', 'running', 'waiting', 'completed',
                                    'failed', 'skipped', 'canceled')),
    attempt       INT         NOT NULL DEFAULT 1,
    input         JSONB,
    output        JSONB,
    error         JSONB,
    started_at    TIMESTAMPTZ,
    ended_at      TIMESTAMPTZ
);

-- ============================================================
-- Indexes for common query patterns
-- ============================================================
CREATE INDEX idx_workspaces_tenant        ON workspaces(tenant_id);
CREATE INDEX idx_wf_definitions_workspace ON workflow_definitions(workspace_id);
CREATE INDEX idx_wf_versions_definition   ON workflow_versions(workflow_definition_id);
CREATE INDEX idx_wf_manifests_version     ON workflow_manifests(workflow_version_id);
CREATE INDEX idx_graph_specs_entry_node   ON graph_specs(entry_node_id);
CREATE INDEX idx_node_specs_graph         ON node_specs(graph_spec_id);
CREATE INDEX idx_edge_specs_source        ON edge_specs(source_node_id);
CREATE INDEX idx_edge_specs_target        ON edge_specs(target_node_id);
CREATE INDEX idx_agent_definitions_ws     ON agent_definitions(workspace_id);
CREATE INDEX idx_agent_versions_def       ON agent_versions(agent_definition_id);
CREATE INDEX idx_tool_definitions_ws      ON tool_definitions(workspace_id);
CREATE INDEX idx_tool_versions_def        ON tool_versions(tool_definition_id);
CREATE INDEX idx_runs_wf_version          ON runs(workflow_version_id);
CREATE INDEX idx_runs_status              ON runs(status);
CREATE INDEX idx_runs_started_by          ON runs(started_by);
CREATE INDEX idx_step_runs_run            ON step_runs(run_id);
CREATE INDEX idx_step_runs_node           ON step_runs(node_spec_id);
CREATE INDEX idx_step_runs_status         ON step_runs(status);
