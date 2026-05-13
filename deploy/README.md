# AgentRail 本地开发环境

基于 Docker Compose 的一键本地开发环境，包含以下基础设施：

| 服务 | 说明 | 端口 |
|------|------|------|
| **Postgres 16 + pgvector** | 主数据库 + 向量存储 | `5432` |
| **Temporal Server** | 可靠工作流引擎 | `7233` (gRPC) |
| **Temporal Web UI** | 工作流可视化 | `8080` |
| **Temporal Admin Tools** | tctl / temporal CLI | — |
| **MinIO** | S3 兼容对象存储 | `9000` (API) / `9001` (Console) |

## 快速开始

### 1. 配置环境变量

```bash
cd deploy
cp .env.example .env
# 按需修改 .env 中的密码等配置
```

### 2. 启动所有服务

```bash
docker compose up -d
```

首次启动 Temporal auto-setup 镜像需要约 30-60 秒完成数据库 schema 初始化。

### 3. 验证服务状态

```bash
# 查看所有容器状态
docker compose ps

# 检查 Postgres 连接
docker compose exec postgres psql -U agentrail -c '\l'

# 检查 pgvector 扩展
docker compose exec postgres psql -U agentrail -c 'SELECT extname FROM pg_extension WHERE extname = %(quote)svector%(quote)s'

# 检查 Temporal
docker compose exec temporal-admin-tools temporal operator cluster health

# 检查 MinIO bucket
docker compose exec minio mc ls local/agentrail
```

### 4. 访问 Web 界面

| 服务 | 地址 |
|------|------|
| Temporal Web UI | http://localhost:8080 |
| MinIO Console | http://localhost:9001 |

MinIO Console 登录凭据默认：
- Username: `minioadmin`
- Password: `minioadmin123`

## 连接信息（开发用）

```yaml
# Postgres (应用数据库)
spring.datasource.url: jdbc:postgresql://localhost:5432/agentrail
spring.datasource.username: agentrail
spring.datasource.password: agentrail_dev

# Temporal
temporal.address: localhost:7233
temporal.namespace: default

# MinIO (S3)
minio.endpoint: http://localhost:9000
minio.access-key: minioadmin
minio.secret-key: minioadmin123
minio.bucket: agentrail
```

## 常用运维命令

```bash
# 查看 Temporal 命名空间
docker compose exec temporal-admin-tools temporal operator namespace list

# 创建新命名空间
docker compose exec temporal-admin-tools temporal operator namespace create my-namespace

# 查看 MinIO buckets
docker compose exec minio mc ls local

# 停止所有服务（保留数据）
docker compose down

# 停止并清除所有数据
docker compose down -v
```

## 目录结构

```
deploy/
├── docker-compose.yml     # 编排文件
├── .env.example           # 环境变量模板
├── .env                   # 本地环境变量（git ignored）
├── postgres/
│   └── init.sql           # 数据库初始化（pgvector + Temporal DB）
└── README.md              # 本文件
```

## 注意事项

- **数据持久化**：Postgres 和 MinIO 数据存储在 Docker volumes 中，`docker compose down` 不会丢失数据
- **Temporal 冷启动**：首次启动较慢（需要初始化数据库 schema），后续启动正常
- **pgvector 版本**：使用 `pgvector/pgvector:pg16` 镜像，兼容 pgvector 0.7+
- **重置环境**：`docker compose down -v` 会删除所有 volumes，等效于全新环境
