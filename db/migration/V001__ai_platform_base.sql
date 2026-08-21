-- AI Prompt Studio 基础迁移。
-- RuoYi 系统表由 docker-entrypoint-initdb.d/01-ruoyi.sql 初始化，本目录只管理 AI 业务表。

create extension if not exists pgcrypto;

create table if not exists ai_platform_migration_meta
(
    id          bigint primary key,
    schema_name varchar(64) not null,
    convention  varchar(32) not null,
    description varchar(500) not null,
    created_at  timestamptz not null default now()
);

insert into ai_platform_migration_meta (id, schema_name, convention, description)
values (1, 'public', 'ai-v1', 'AI 业务表使用 bigint 主键、审计字段和 del_flag 逻辑删除约定')
on conflict (id) do nothing;

comment on table ai_platform_migration_meta is 'AI 平台数据库约定元数据';
