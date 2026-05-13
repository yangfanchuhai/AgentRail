-- ============================================================
-- AgentRail — PostgreSQL 初始化脚本
-- 在容器首次启动时由 docker-entrypoint 自动执行
-- ============================================================

-- pgvector 扩展（向量存储 / embedding）
CREATE EXTENSION IF NOT EXISTS vector;

-- ----------------------------------------------------------
-- Temporal 需要的数据库（auto-setup 镜像会自动创建 schema）
-- ----------------------------------------------------------
SELECT 'CREATE DATABASE temporal'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'temporal')\gexec

SELECT 'CREATE DATABASE temporal_visibility'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'temporal_visibility')\gexec

-- 初始化完成
DO $$
BEGIN
    RAISE NOTICE 'AgentRail init: pgvector extension + Temporal databases ready.';
END
$$;
