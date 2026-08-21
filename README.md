# AI Prompt Studio

面向 Java/Spring Boot 企业应用的 AI Prompt、模型和工作流治理平台。

本项目聚焦企业私有化部署场景，提供项目级 RBAC、模型配置、Prompt 版本管理、在线调试、低代码工作流、API 发布、运行追踪和成本统计能力。

## 当前状态

项目处于规格设计阶段。实现范围、任务拆分和验收标准见 [SPEC.md](./SPEC.md)。

## 目标技术栈

- Java 21、Spring Boot 3、MyBatis-Plus、Sa-Token
- PostgreSQL、Redis、MinIO、pgvector
- Vue 3、TypeScript、Vite、Element Plus、Vue Flow
- Docker Compose

## 设计原则

- 模块化单体优先，避免过早拆分微服务。
- 已发布 Prompt 和工作流版本不可变。
- 系统 RBAC 与项目数据权限分层实现。
- 后端始终校验权限、资源版本和工作流定义，不能信任前端。
- 敏感凭证加密保存，运行日志支持脱敏。
