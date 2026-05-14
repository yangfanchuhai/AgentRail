# AgentRail

**带有工作流约束的智能体流程运行时 (Agentic Process Runtime)**

AgentRail 让你定义工作流图，将 AI 智能体（Agent）绑定到图中的阶段节点，由策略（Policy）强制治理约束，实现可审计、可恢复的企业级 Agent 编排。

## 架构

```
┌─────────────────────────────────────────────────────────┐
│                    AgentRail 架构                        │
├───────────────┬──────────────┬──────────────────────────┤
│  控制面 (API)  │  数据面       │  执行面 (Worker)          │
│  Java/Spring  │  Java/Temporal│  Python/Temporal          │
│  port 8081    │  port 8082   │  Temporal Task Queue      │
├───────────────┼──────────────┼──────────────────────────┤
│ Tenant CRUD   │ Start Run    │ load_manifest             │
│ Workflow CRUD │ Cancel Run   │ execute_node (agent/tool) │
│ Agent CRUD    │ Run Status   │ step lifecycle callbacks  │
│ Tool CRUD     │ StepRun CRUD │ graph walk orchestration  │
│ Publish+Valid │ Temporal Clt │ LangGraph (future)        │
└───────────────┴──────────────┴──────────────────────────┘
         │              │              │
         └──────┬───────┴──────────────┘
                │
     ┌──────────┼──────────┐
     │          │          │
  PostgreSQL  Temporal    MinIO
  16+pgvector Server     S3存储
```

## 技术栈

| 组件 | 技术 |
|------|------|
| 控制面 API | Spring Boot 3.4, JPA, PostgreSQL, Flyway |
| 数据面 Service | Spring Boot 3.4, Temporal Client, JPA |
| 执行面 Worker | Python 3.12, Temporal SDK, SQLAlchemy, httpx |
| 工作流引擎 | Temporal 1.26 |
| 数据库 | PostgreSQL 16 + pgvector |
| 对象存储 | MinIO |
| 智能体执行 | LangGraph (Phase 3, 规划中) |

## 项目结构

```
AgentRail/
├── agentrail-api/          # 控制面 — CRUD API (Spring Boot, port 8081)
├── agentrail-service/      # 数据面 — Run API + Temporal Client (port 8082)
├── agentrail-worker/       # 执行面 — Temporal Worker (Python)
├── deploy/                 # Docker Compose + 测试脚本
├── docs/                   # 设计文档
└── pom.xml                 # Maven 父 POM
```

## 快速开始

### 前置要求

- Docker + Docker Compose
- JDK 21 (本地开发)
- Python 3.12+ (本地开发 Worker)

### 启动基础设施

```bash
cd deploy
docker-compose up -d postgres temporal temporal-ui minio
```

### 本地开发 (API + Service)

```bash
# 编译
mvn clean package -DskipTests

# 启动控制面 (port 8081)
java -jar agentrail-api/target/agentrail-api-0.1.0-SNAPSHOT.jar

# 启动数据面 (port 8082)
java -jar agentrail-service/target/agentrail-service-0.1.0-SNAPSHOT.jar
```

### 本地开发 (Worker)

```bash
cd agentrail-worker
pip install -e .
agentrail-worker
```

### 全栈 Docker 启动

```bash
cd deploy
docker-compose up -d
```

### 端到端测试

```bash
bash deploy/e2e-test.sh
```

## API 端点

### 控制面 (port 8081)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/tenants` | 创建租户 |
| POST | `/api/v1/tenants/{id}/workspaces` | 创建工作空间 |
| CRUD | `/api/v1/workspaces/{ws}/workflows` | 工作流定义 |
| POST | `.../workflows/{wf}/versions` | 创建版本 (含 Graph/Node/Edge) |
| POST | `.../versions/{v}/publish` | 发布校验 |
| CRUD | `/api/v1/workspaces/{ws}/agents` | Agent 定义 + 版本 |
| CRUD | `/api/v1/workspaces/{ws}/tools` | Tool 定义 + 版本 |

### 数据面 (port 8082)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/runs` | 启动 Run (→ Temporal) |
| GET | `/api/v1/runs/{id}` | 查询 Run 状态 |
| POST | `/api/v1/runs/{id}/cancel` | 取消 Run |
| GET | `/api/v1/runs/{id}/steps` | 查看 StepRuns |
| POST | `/api/v1/internal/runs/{id}/status` | Worker 回调 (内部) |
| POST | `/api/v1/internal/runs/{id}/steps` | Worker 创建 StepRun |

## 端口一览

| 服务 | 端口 |
|------|------|
| agentrail-api | 8081 |
| agentrail-service | 8082 |
| PostgreSQL | 5432 |
| Temporal gRPC | 7233 |
| Temporal UI | 8080 |
| MinIO API | 9000 |
| MinIO Console | 9001 |

## License

All rights reserved. Private project.
