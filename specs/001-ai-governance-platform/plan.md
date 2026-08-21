# 实施计划：企业级 AI 能力治理平台

**分支**：`001-ai-governance-platform` | **日期**：2026-08-21 | **规格**：[spec.md](./spec.md)

## 摘要

基于 RuoYi-Vue-Plus 6.x 建立模块化单体，在保留其认证、系统 RBAC、审计和前端管理能力的
基础上，增加项目隔离、模型网关、Prompt 生命周期、外部调用、轻量 DAG 工作流和执行追踪。
后端业务按领域模块拆分但统一部署；工作流执行先采用进程内异步执行，并通过明确端口为未来
拆分 Worker 保留演进空间。

## 技术上下文

**语言与版本**：Java 21；TypeScript 6；Node.js 20+；pnpm 10+

**核心依赖**：RuoYi-Vue-Plus 6.x、Spring Boot 4.1、Sa-Token、MyBatis-Plus、Spring AI 2.0、
Vue 3、Element Plus、Vue Flow、Resilience4j、Flyway、SpringDoc

**存储**：PostgreSQL 17 为主数据存储；Redis 8 用于缓存、限流和短期执行协调；JSONB 保存
动态 Schema、模型能力和工作流定义

**测试**：JUnit 5、Mockito、Spring Boot Test、Testcontainers、Vitest、Vue Test Utils、
Playwright；ArchUnit 校验模块依赖

**目标平台**：Linux 容器化部署；Windows/Linux 本地开发；桌面浏览器管理端

**项目类型**：前后端分离的模块化单体 Web 应用

**性能目标**：普通管理查询在验收环境 p95 小于 500ms；模型调用在供应商返回首块内容后
500ms 内转发；单实例支持 100 个并发模型流和 50 个并发工作流；执行列表支持百万级记录
分页查询

**约束**：所有资源查询包含项目边界；已发布快照不可更新；密钥不明文持久化；工作流禁止
任意代码执行；外部 HTTP 节点必须防止 SSRF；业务模块不得直接依赖具体供应商 SDK

**规模范围**：单组织、最多 1,000 用户、1,000 项目、10 万 Prompt/工作流版本、每月 100 万
执行记录；超出后再评估执行服务和分析存储拆分

## 宪章检查

| 门禁 | 计划响应 | 结果 |
|---|---|---|
| 服务端权限与项目隔离 | 统一 `ProjectAccessService`，接口与查询双重约束 | 通过 |
| 发布版本不可变 | 主资源、版本、发布指针分表；版本无更新入口 | 通过 |
| 每次执行可追踪 | 执行与节点执行均保存 executionId/traceId | 通过 |
| 独立可测试增量 | 模块按用户故事和依赖顺序交付 | 通过 |
| 最小充分设计 | 模块化单体、进程内 DAG，不引入微服务和消息队列 | 通过 |
| 禁止任意代码执行 | MVP 节点白名单，不提供脚本节点 | 通过 |

Phase 1 设计复查：数据模型将版本与发布指针分离；外部契约只暴露已发布资源；所有业务表
均关联项目；没有发现宪章例外。

## 模块边界

```text
system（RuoYi 基座）
  └─ 用户、系统角色、菜单权限、操作日志
ai-project
  └─ 项目、成员、项目角色、项目访问判定
ai-model
  └─ 供应商、模型、加密凭证、统一模型网关
ai-prompt
  └─ Prompt、变量、版本、测试资格、发布与回滚
ai-execution
  └─ 执行记录、节点记录、用量费用、内容留存策略
ai-openapi
  └─ 外部凭证、鉴权、限流、已发布资源调用
ai-workflow
  └─ 工作流定义、DAG 校验、节点执行器、发布
ai-audit
  └─ AI 领域审计事件查询；写入通过领域事件完成
```

允许的核心依赖方向：

```text
project <- model
project <- prompt -> model
project <- workflow -> prompt/model
prompt/workflow -> execution
openapi -> prompt/workflow/execution
各业务模块 -> audit（通过事件，不反向依赖）
```

禁止模块通过其他模块的 Mapper 或数据库表直接读写；跨模块只调用公开应用服务或消费领域
事件。首版领域事件采用 Spring 进程内事件与事务后监听器。

## 项目结构

### 本功能文档

```text
specs/001-ai-governance-platform/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md
```

### 源代码

```text
ai-prompt-studio/
├── pom.xml
├── ruoyi-admin/                         # 单体启动模块
├── ruoyi-common/                        # RuoYi 公共基础能力
├── ruoyi-modules/
│   ├── ruoyi-system/                    # 现有系统 RBAC
│   ├── ai-project/
│   ├── ai-model/
│   ├── ai-prompt/
│   ├── ai-execution/
│   ├── ai-openapi/
│   ├── ai-workflow/
│   └── ai-audit/
├── plus-ui/                             # Vue 管理端
│   └── src/views/ai/
│       ├── project/
│       ├── model/
│       ├── prompt/
│       ├── workflow/
│       ├── execution/
│       └── audit/
├── db/migration/                        # Flyway 迁移与初始化菜单
├── deploy/compose/                      # 本地/验收环境
└── tests/
    ├── architecture/
    ├── integration/
    └── e2e/
```

每个后端业务模块内部统一采用：

```text
controller/     接口与入参转换
application/    用例编排、事务边界、公开模块服务
domain/         聚合、值对象、策略、领域事件
infrastructure/ Mapper、供应商适配、缓存等
```

**结构决策**：保留 RuoYi 6.x 的 Maven 多模块和前端目录，仅新增 `ai-*` 领域模块，不修改
系统模块的认证核心。部署仍是一个后端进程和一个前端应用。

## 交付顺序

1. P0：引入并裁剪 RuoYi 6.x，建立构建、数据库、测试和模块约束。
2. P1：`ai-project`，完成项目与项目级权限闭环。
3. P2：`ai-model`，完成凭证保护和统一模型调用。
4. P3：`ai-prompt` + `ai-execution`，完成创建、测试、版本与运行记录。
5. P4：发布、回滚、`ai-openapi` 和审计闭环。
6. P5：`ai-workflow`，完成六类节点、DAG 校验和节点追踪。
7. P6：安全、性能、端到端验收和部署文档。

## 复杂度跟踪

没有需要偏离宪章的复杂设计。Redis、模块事件和 JSONB 均直接服务于当前限流、隔离和动态
工作流需求；未引入独立消息队列、搜索集群或微服务。
