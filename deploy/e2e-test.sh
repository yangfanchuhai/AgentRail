#!/usr/bin/env bash
# ============================================================
# AgentRail — 端到端 API 测试脚本
# 用法: bash e2e-test.sh [API_BASE] [SERVICE_BASE]
# 默认: API=http://localhost:8081  SERVICE=http://localhost:8082
# ============================================================
set -euo pipefail

API="${1:-http://localhost:8081}"
SERVICE="${2:-http://localhost:8082}"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; }
info() { echo -e "${YELLOW}→ $1${NC}"; }

# ── Helper ──
post() {
  local url="$1" body="$2"
  curl -sf -X POST "$url" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: e2e-test" \
    -d "$body"
}

get() {
  curl -sf "$1"
}

# ── 1. Health Check ──
info "Checking health endpoints..."
API_HEALTH=$(get "$API/actuator/health" 2>/dev/null || echo "FAIL")
SVC_HEALTH=$(get "$SERVICE/actuator/health" 2>/dev/null || echo "FAIL")

[[ "$API_HEALTH" == *"UP"* ]] && pass "API health OK" || fail "API health: $API_HEALTH"
[[ "$SVC_HEALTH" == *"UP"* ]] && pass "Service health OK" || fail "Service health: $SVC_HEALTH"

# ── 2. Create Tenant ──
info "Creating tenant..."
TENANT=$(post "$API/api/v1/tenants" '{"name":"E2E Test Corp","slug":"e2e-test"}')
TENANT_ID=$(echo "$TENANT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "Tenant created: $TENANT_ID"

# ── 3. Create Workspace ──
info "Creating workspace..."
WS=$(post "$API/api/v1/tenants/$TENANT_ID/workspaces" '{"name":"Test Workspace","slug":"test-ws"}')
WS_ID=$(echo "$WS" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "Workspace created: $WS_ID"

# ── 4. Create Workflow Definition ──
info "Creating workflow definition..."
WF=$(post "$API/api/v1/workspaces/$WS_ID/workflows" '{"key":"hello-flow","name":"Hello Flow","description":"E2E test workflow"}')
WF_ID=$(echo "$WF" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "WorkflowDefinition created: $WF_ID"

# ── 5. Create Workflow Version with Graph ──
info "Creating workflow version with graph..."
VERSION=$(post "$API/api/v1/workspaces/$WS_ID/workflows/$WF_ID/versions" '{
  "version": "1.0.0",
  "graph": {
    "key": "hello-graph",
    "name": "Hello Graph",
    "entryNodeKey": "start"
  },
  "nodes": [
    {"key": "start", "type": "input", "label": "Start Node"},
    {"key": "agent1", "type": "agent", "label": "Hello Agent"},
    {"key": "end", "type": "output", "label": "End Node"}
  ],
  "edges": [
    {"sourceNodeKey": "start", "targetNodeKey": "agent1"},
    {"sourceNodeKey": "agent1", "targetNodeKey": "end"}
  ]
}')
VER_ID=$(echo "$VERSION" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "WorkflowVersion created: $VER_ID"

# ── 6. Publish Version ──
info "Publishing version..."
PUBLISHED=$(post "$API/api/v1/workspaces/$WS_ID/workflows/$WF_ID/versions/$VER_ID/publish" '{}')
pass "Version published: $(echo "$PUBLISHED" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('lifecycleStatus','?'))")"

# ── 7. Create Agent Definition ──
info "Creating agent definition..."
AGENT=$(post "$API/api/v1/workspaces/$WS_ID/agents" '{"key":"hello-agent","name":"Hello Agent","description":"Test agent"}')
AGENT_DEF_ID=$(echo "$AGENT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "AgentDefinition created: $AGENT_DEF_ID"

# ── 8. Create Agent Version ──
info "Creating agent version..."
AGENT_VER=$(post "$API/api/v1/workspaces/$WS_ID/agents/$AGENT_DEF_ID/versions" '{
  "version": "1.0.0",
  "role": "assistant",
  "goal": "Greet the user",
  "systemInstruction": "You are a friendly greeting agent.",
  "planningMode": "react",
  "maxIterations": 5
}')
pass "AgentVersion created"

# ── 9. Create Tool Definition ──
info "Creating tool definition..."
TOOL=$(post "$API/api/v1/workspaces/$WS_ID/tools" '{"key":"echo-tool","name":"Echo Tool","description":"Echoes input"}')
TOOL_DEF_ID=$(echo "$TOOL" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "ToolDefinition created: $TOOL_DEF_ID"

# ── 10. Create Tool Version ──
info "Creating tool version..."
TOOL_VER=$(post "$API/api/v1/workspaces/$WS_ID/tools/$TOOL_DEF_ID/versions" '{
  "version": "1.0.0",
  "type": "function",
  "inputSchema": {"type":"object","properties":{"message":{"type":"string"}}},
  "outputSchema": {"type":"object","properties":{"echo":{"type":"string"}}},
  "sideEffectLevel": "none"
}')
pass "ToolVersion created"

# ── 11. Start a Run ──
info "Starting run (requires Temporal + Worker)..."
RUN=$(post "$SERVICE/api/v1/runs" "{\"workflowVersionId\":\"$VER_ID\",\"input\":{\"message\":\"hello\"}}" 2>/dev/null || echo '{"error":"temporal not available"}')
if echo "$RUN" | python3 -c "import sys,json; d=json.load(sys.stdin); 'id' in d and sys.exit(0) or sys.exit(1)" 2>/dev/null; then
  RUN_ID=$(echo "$RUN" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  pass "Run started: $RUN_ID"
  
  # Wait and check status
  sleep 3
  RUN_STATUS=$(get "$SERVICE/api/v1/runs/$RUN_ID")
  STATUS=$(echo "$RUN_STATUS" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','unknown'))")
  info "Run status: $STATUS"
  
  # Check step runs
  STEPS=$(get "$SERVICE/api/v1/runs/$RUN_ID/steps" 2>/dev/null || echo "[]")
  STEP_COUNT=$(echo "$STEPS" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
  info "StepRuns created: $STEP_COUNT"
else
  fail "Run start failed (Temporal not available?): $RUN"
fi

# ── Summary ──
echo ""
echo "============================================"
echo -e "${GREEN}E2E Test Complete${NC}"
echo "Tenant:   $TENANT_ID"
echo "Workspace: $WS_ID"
echo "Workflow:  $WF_ID"
echo "Version:   $VER_ID"
echo "============================================"
