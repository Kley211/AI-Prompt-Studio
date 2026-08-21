# 数据模型：企业级 AI 能力治理平台

## 设计规则

- 所有 AI 业务实体都必须包含 `project_id`，系统级供应商配置除外。
- 所有业务表使用 `id`、创建/修改人、创建/修改时间和逻辑删除字段。
- 已发布版本只允许读取；发布指针可以变更，但必须写审计事件。
- 密钥正文不进入普通查询 DTO、审计内容或执行内容。
- 动态配置使用 JSONB，并在应用层和发布前进行结构校验。

## 核心实体

### project：项目

| 字段 | 类型 | 规则 |
|---|---|---|
| id | bigint | 主键 |
| code | varchar(64) | 全局唯一、创建后不可变 |
| name | varchar(128) | 必填 |
| description | varchar(500) | 可选 |
| status | varchar(20) | ACTIVE、ARCHIVED |
| retention_mode | varchar(20) | FULL、MASKED、METADATA_ONLY |
| owner_id | bigint | 必须引用有效用户 |

### project_member：项目成员

| 字段 | 类型 | 规则 |
|---|---|---|
| project_id | bigint | 与 user_id 联合唯一 |
| user_id | bigint | 引用系统用户 |
| role | varchar(20) | OWNER、ADMIN、DEVELOPER、PUBLISHER、VIEWER |
| status | varchar(20) | ACTIVE、REMOVED |

项目必须至少有一个 OWNER；OWNER 移除前必须先转移所有权。

### model_provider：模型供应商

保存供应商名称、协议类型、Base URL、启用状态和能力说明。供应商不属于项目，但项目通过
`project_model` 建立可用范围。

### model_credential：供应商凭证

保存供应商引用、密文、密钥版本和启用状态。密文只能由模型网关在运行时读取；响应只返回
名称、前缀和状态。

### model：模型

保存供应商、模型编码、显示名、类型、能力 JSON、上下文窗口、输入/输出价格和启用状态。
能力至少包括 streaming、json_mode、tool_calling、vision。

### project_model：项目模型授权

保存 project_id、model_id、别名和启用状态。开发者只能选择当前项目授权且启用的模型。

### prompt：Prompt 主资源

保存 project_id、稳定 code、名称、描述、当前草稿版本 ID、当前发布版本 ID 和状态。
`project_id + code` 必须唯一。

### prompt_version：Prompt 版本

保存 prompt_id、版本号、系统模板、用户模板、变量 JSON、输入/输出 Schema、模型引用、模型
参数 JSON、版本状态、变更说明和成功测试标记。PUBLISHED 版本不允许 UPDATE/DELETE。

状态：DRAFT、TESTABLE、PUBLISHED、ARCHIVED。

### workflow：工作流主资源

保存 project_id、稳定 code、名称、描述、当前草稿版本 ID、当前发布版本 ID 和状态。

### workflow_version：工作流版本

保存 workflow_id、版本号、定义 JSON、输入/输出 Schema、状态、变更说明和校验摘要。定义
包含 nodes、edges、node settings 和 mappings。PUBLISHED 版本不可修改。

### external_credential：外部调用凭证

保存 project_id、名称、key_prefix、key_hash、过期时间、启用状态和最后使用时间。原始凭证
只在创建响应中返回一次。

### execution：执行记录

保存 execution_id、trace_id、project_id、资源类型/ID/版本、发起方类型、状态、开始/结束时间、
耗时、输入/输出 Token、预估费用、留存模式和脱敏错误信息。

状态：PENDING、RUNNING、SUCCEEDED、FAILED、CANCELLED。

### node_execution：节点执行记录

保存 execution_id、node_id、节点类型、状态、输入/输出摘要、开始/结束时间、耗时、重试次数
和脱敏错误。

状态：PENDING、RUNNING、SUCCEEDED、FAILED、SKIPPED。

### audit_event：审计事件

保存 actor、action、target_type、target_id、project_id、result、occurred_at、trace_id 和
脱敏 metadata。应用层不得提供普通更新或删除接口。

## 关系

```text
User 1---N ProjectMember N---1 Project
Project 1---N Prompt 1---N PromptVersion
Project 1---N Workflow 1---N WorkflowVersion
Project 1---N ProjectModel N---1 Model N---1 ModelProvider
ModelProvider 1---N ModelCredential
Project 1---N ExternalCredential
Project 1---N Execution 1---N NodeExecution
```

## 关键状态规则

```text
Prompt/Workflow:
DRAFT -> TESTABLE -> PUBLISHED -> ARCHIVED
TESTABLE -> DRAFT
PUBLISHED -> ARCHIVED（创建新发布指针，不修改版本内容）

Execution:
PENDING -> RUNNING -> SUCCEEDED|FAILED|CANCELLED
```

发布事务必须检查：发布权限、项目成员状态、版本结构有效、至少一次成功测试、引用模型
仍启用。回滚只更新主资源的发布指针并写入 rollback 审计事件。

## 索引与约束

- `project_member(project_id, user_id)` 唯一。
- `prompt(project_id, code)` 唯一；`workflow(project_id, code)` 唯一。
- `prompt_version(prompt_id, version_no)` 唯一；`workflow_version(workflow_id, version_no)` 唯一。
- `execution(project_id, created_at)`、`execution(trace_id)`、`execution(resource_id, created_at)` 建索引。
- 所有项目资源查询必须先应用 `project_id` 过滤，再进行分页或模糊搜索。
