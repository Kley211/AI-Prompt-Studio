# AI Prompt Studio

面向 Java/Spring Boot 企业应用的 AI Prompt、模型和工作流治理平台。

本项目聚焦企业私有化部署场景，提供项目级 RBAC、模型配置、Prompt 版本管理、在线调试、低代码工作流、API 发布、运行追踪和成本统计能力。

## 当前状态

项目采用 GitHub Spec Kit 的规格驱动开发流程，当前已完成 RuoYi-Vue-Plus 6.0.0
后端与 Vue 3 前端基线接入。产品范围和验收标准见 [规格索引](./SPEC.md)，实施顺序见
[实施任务清单](./specs/001-ai-governance-platform/tasks.md)。

## 目标技术栈

- Java 21、Spring Boot 4.1、MyBatis-Plus、Sa-Token、Maven Wrapper
- PostgreSQL、Redis、MinIO、pgvector
- Vue 3、TypeScript、Vite、Element Plus、Vue Flow
- Docker Compose

## 本地开发基线

- 后端：在仓库根目录执行 `./mvnw.cmd package -DskipTests -Dcheckstyle.skip=true`
- 前端：进入 `plus-ui/` 执行 `pnpm install --frozen-lockfile` 和 `pnpm build`
- 运行依赖：执行 `docker compose -f deploy/compose/docker-compose.yml up -d`
- Java、Node.js 和 pnpm 版本约束见 `.tool-versions`

## 设计原则

- 模块化单体优先，避免过早拆分微服务。
- 已发布 Prompt 和工作流版本不可变。
- 系统 RBAC 与项目数据权限分层实现。
- 后端始终校验权限、资源版本和工作流定义，不能信任前端。
- 敏感凭证加密保存，运行日志支持脱敏。
