# AgentRail

> Workflow 约束下的 Agentic Process Runtime

面向企业复杂任务的 Agentic Workflow Platform。

## 核心设计哲学

- **Workflow** 规定"有哪些阶段，以及阶段之间允许如何转换"
- **Agent** 决定"在某个阶段内如何根据目标、状态、skills、tools、models 完成任务"
- **Policy** 决定"什么情况下允许这么做、是否需要审批、是否超出预算或权限"

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    客户端 / 业务系统                       │
│              Web Console / Biz Backend / CI              │
├──────────────┬──────────────────┬────────────────────────┤
│  控制面       │     数据面        │       执行面            │
│  agentrail   │   agentrail      │     agentrail          │
│  -api        │  -service        │    -worker             │
│  (Java 21)   │  (Java 21)       │   (Python 3.12)        │
│              │                  │                        │
│  定义/注册    │  Run生命周期      │  Agent执行              │
│  治理/身份    │  Signal/HumanTask│  Tool/MCP/Model调用     │
│              │  查询/可观测      │  LangGraph + Temporal   │
├──────────────┴──────────────────┴────────────────────────┤
│  Postgres 16 + pgvector  │  Temporal  │  MinIO (S3)      │
└─────────────────────────────────────────────────────────┘
```

## 项目结构

```
AgentRail/
├── agentrail-api/          # 控制面 — Java 21 + Spring Boot 3
├── agentrail-service/      # 数据面 — Java 21 + Spring Boot 3
├── agentrail-worker/       # 执行面 — Python 3.12 + Temporal + LangGraph
├── agentrail-contracts/    # 跨语言共享契约 (JSON Schema)
├── deploy/                 # Docker Compose 本地开发环境
│   ├── docker-compose.yml
│   ├── postgres/init.sql
│   └── .env.example
├── docs/                   # 设计文档
├── pom.xml                 # Maven 父 POM
└── README.md
```

## 快速开始

### 1. 启动本地基础设施

```bash
cd deploy
cp .env.example .env
# 按需修改 .env 中的密码
docker compose up -d
```

启动后可用：
- Postgres: `localhost:5432` (user: agentrail, db: agentrail)
- Temporal Web UI: http://localhost:8080
- MinIO Console: http://localhost:9001 (user: minioadmin)

### 2. 启动 Java 服务

```bash
# 控制面
cd agentrail-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 数据面
cd agentrail-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- agentrail-api: http://localhost:8081
- agentrail-service: http://localhost:8082

### 3. 启动 Python Worker

```bash
cd agentrail-worker
uv sync
uv run agentrail-worker
```

## 技术栈

| 组件 | 技术 |
|------|------|
| 控制面 / 数据面 | Java 21, Spring Boot 3.4, jOOQ, Spring Data JPA, Flyway |
| 执行面 | Python 3.12, Temporal Python SDK, LangGraph, Pydantic v2 |
| 前端 | TypeScript, React, Ant Design (计划中) |
| 数据库 | PostgreSQL 16 + pgvector |
| 工作流引擎 | Temporal |
| 对象存储 | MinIO (S3-compatible) |
| 契约 | JSON Schema / OpenAPI |

## License

Private — All rights reserved.
