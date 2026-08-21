# 任务清单：企业级 AI 能力治理平台

**输入**：`spec.md`、`plan.md`、`research.md`、`data-model.md`、`contracts/openapi.yaml`、`quickstart.md`

**执行规则**：每个任务都必须包含复选框、任务编号、必要的并行标记、用户故事标记（故事阶段）和明确文件路径。

## 阶段 1：项目初始化

**目标**：建立可编译、可测试、可运行的模块化单体骨架。

- [X] T001 获取 RuoYi-Vue-Plus 6.x 源码基线并在仓库根目录建立 `pom.xml`、`ruoyi-admin/`、`ruoyi-modules/` 和 `plus-ui/`。
- [X] T002 [P] 固定 Java 21、Node.js 20+、pnpm 10+ 和 Maven Wrapper 版本，更新 `.tool-versions`、`.mvn/` 和根目录 `README.md`。
- [X] T003 [P] 创建 `deploy/compose/docker-compose.yml`，定义 PostgreSQL 17 和 Redis 8 的本地开发服务及健康检查。
- [X] T004 [P] 创建 `db/migration/README.md` 和 Flyway 迁移目录，约定迁移命名、回滚限制和初始化菜单管理方式。
- [X] T005 [P] 创建 `tests/architecture/`、`tests/integration/` 和 `tests/e2e/` 目录，并配置基础测试脚本。
- [ ] T006 运行基线构建和登录冒烟测试，记录 `docs/verification/baseline.md`。

## 阶段 2：基础设施与跨模块约束

**目标**：完成所有用户故事依赖的数据库、错误、日志、权限和安全基础能力。此阶段完成前不得开始业务故事实现。

- [ ] T007 创建 `db/migration/V001__ai_platform_base.sql`，建立审计字段、逻辑删除、时间和 ID 规范。
- [ ] T008 [P] 在 `ruoyi-modules/ai-common/` 创建统一错误码、分页、Trace ID、脱敏和密钥保护基础类。
- [ ] T009 [P] 在 `ruoyi-modules/ai-common/` 创建项目访问端口 `ProjectAccessService` 和公共项目资源查询约束。
- [ ] T010 [P] 配置 `ruoyi-modules/ai-common/` 的统一异常响应、结构化日志和敏感字段过滤。
- [ ] T011 [P] 配置 `tests/architecture/ModuleBoundaryTest.java`，禁止跨模块直接引用 Mapper、Entity 和数据库表。
- [ ] T012 创建 `db/migration/V002__ai_menu_permissions.sql`，初始化 AI 项目、模型、Prompt、工作流、执行和审计菜单权限。
- [ ] T013 [P] 创建 `deploy/compose/.env.example` 和 `application-local.yml.example`，只包含占位符，不包含真实密钥。
- [ ] T014 [P] 配置 `ruoyi-admin/src/main/resources/application.yml` 的数据库、Redis、Flyway、加密主密钥和执行线程池参数。
- [ ] T015 运行基础安全检查，确认提交内容不包含 API Key、密码、Token 和生产配置，并记录 `docs/verification/security-baseline.md`。

**检查点**：基座可启动；未登录返回 401；功能权限不足返回 403；Trace ID 能贯穿一次请求；架构测试通过。

## 阶段 3：用户故事 1——项目工作空间与权限（P1，MVP）

**目标**：完成项目、成员和项目级数据隔离，使不同项目和角色的访问可以独立验收。

**独立验收**：创建两个项目和不同成员，验证授权操作成功、观察者修改被拒绝、跨项目资源不泄露。

- [ ] T016 [P] [US1] 创建 `db/migration/V003__ai_project.sql`，建立 `ai_project` 和 `ai_project_member` 表、唯一约束和索引。
- [ ] T017 [P] [US1] 创建 `ruoyi-modules/ai-project/domain/Project.java`、`ProjectMember.java` 和项目角色值对象。
- [ ] T018 [P] [US1] 创建 `ruoyi-modules/ai-project/infrastructure/ProjectMapper.java`、`ProjectMemberMapper.java` 及映射文件。
- [ ] T019 [US1] 实现 `ruoyi-modules/ai-project/application/ProjectApplicationService.java`，完成项目创建、修改、归档和成员管理事务。
- [ ] T020 [US1] 实现 `ruoyi-modules/ai-project/application/ProjectAccessServiceImpl.java`，覆盖查看、编辑、发布和成员管理判断。
- [ ] T021 [US1] 实现 `ruoyi-modules/ai-project/controller/ProjectController.java` 和 `ProjectMemberController.java`，添加系统权限与项目权限双重校验。
- [ ] T022 [US1] 实现 `plus-ui/src/views/ai/project/` 项目列表、详情、成员和角色管理页面及权限按钮。
- [ ] T023 [P] [US1] 编写 `tests/integration/ProjectAuthorizationIT.java`，覆盖跨项目读取、观察者编辑和移除成员后立即失权。
- [ ] T024 [US1] 编写 `tests/e2e/project-access.spec.ts`，验证项目创建、成员分配和越权操作提示。
- [ ] T025 [US1] 运行项目隔离验收并更新 `docs/verification/us1-project.md`。

**检查点**：US1 不依赖后续 AI 模块即可完成并演示。

## 阶段 4：用户故事 2——模型配置与 Prompt 测试（P1）

**目标**：配置受保护模型，创建变量驱动 Prompt 并在线测试，记录用量和错误。

**独立验收**：使用已批准模型创建 Prompt；缺少必填变量时不调用模型；有效输入可得到结果和执行详情。

- [ ] T026 [P] [US2] 创建 `db/migration/V004__ai_model.sql`，建立供应商、模型、密文凭证和项目模型授权表。
- [ ] T027 [P] [US2] 创建 `ruoyi-modules/ai-model/domain/` 的供应商、模型、凭证和能力值对象。
- [ ] T028 [US2] 实现 `ruoyi-modules/ai-model/infrastructure/SecretCipher.java`，使用部署注入主密钥完成凭证加解密和密钥版本管理。
- [ ] T029 [US2] 定义 `ruoyi-modules/ai-model/application/ChatModelGateway.java`、请求/响应对象和统一模型错误。
- [ ] T030 [US2] 实现 `ruoyi-modules/ai-model/infrastructure/OpenAiCompatibleGateway.java`，支持非流式、SSE 流式、超时和错误归一化。
- [ ] T031 [US2] 实现 `ruoyi-modules/ai-model/application/ModelApplicationService.java`，完成供应商、模型、凭证和项目授权管理。
- [ ] T032 [P] [US2] 创建 `db/migration/V005__ai_prompt.sql`，建立 Prompt 主表、版本表和变量 JSON 字段。
- [ ] T033 [P] [US2] 实现 `ruoyi-modules/ai-prompt/domain/PromptTemplateRenderer.java` 和变量、Schema 校验值对象。
- [ ] T034 [US2] 实现 `ruoyi-modules/ai-prompt/application/PromptApplicationService.java`，完成 Prompt 创建、草稿版本和模板校验。
- [ ] T035 [US2] 实现 `ruoyi-modules/ai-execution/domain/ExecutionRecorder.java` 和 `ExecutionCostCalculator.java`。
- [ ] T036 [US2] 实现 `ruoyi-modules/ai-prompt/application/PromptTestService.java`，串联项目权限、变量校验、模型网关和执行记录。
- [ ] T037 [US2] 实现 `ruoyi-modules/ai-model/controller/ModelController.java`、`ruoyi-modules/ai-prompt/controller/PromptController.java` 和测试 SSE 接口。
- [ ] T038 [US2] 实现 `plus-ui/src/views/ai/model/` 和 `plus-ui/src/views/ai/prompt/` 的模型配置、Prompt 编辑器和调试台。
- [ ] T039 [P] [US2] 编写 `tests/unit/PromptTemplateRendererTest.java`、`ModelGatewayErrorTest.java` 和 `SecretCipherTest.java`。
- [ ] T040 [US2] 编写 `tests/integration/PromptTestIT.java`，验证缺少变量不调用模型、成功调用记录 Token/耗时/费用。
- [ ] T041 [US2] 编写 `tests/e2e/prompt-debug.spec.ts`，验证模型配置、Prompt 调试和错误展示。

## 阶段 5：用户故事 3——Prompt 发布与回滚（P1）

**目标**：实现不可变版本、发布资格、版本比较、发布指针和回滚审计。

**独立验收**：发布两个版本，验证发布版本不可修改，并能回滚且保留全部历史。

- [ ] T042 [US3] 实现 `ruoyi-modules/ai-prompt/domain/PromptVersionLifecycle.java`，定义草稿、可测试、发布和归档状态转移。
- [ ] T043 [US3] 实现 `ruoyi-modules/ai-prompt/application/PromptReleaseService.java`，在事务中校验发布权限、成功测试、模型状态并更新发布指针。
- [ ] T044 [US3] 实现 `ruoyi-modules/ai-prompt/application/PromptVersionCompareService.java`，提供模板、变量、模型参数和 Schema 差异。
- [ ] T045 [US3] 添加 `ruoyi-modules/ai-prompt/infrastructure/PromptVersionMapper.xml` 的只读发布版本约束和乐观锁更新。
- [ ] T046 [US3] 添加 `ruoyi-modules/ai-prompt/controller/PromptReleaseController.java` 的测试、发布、比较、回滚和归档接口。
- [ ] T047 [P] [US3] 实现 `plus-ui/src/views/ai/prompt/PromptVersionPanel.vue`、版本比较和发布回滚交互。
- [ ] T048 [US3] 编写 `tests/integration/PromptReleaseIT.java`，覆盖发布不可变、并发发布、无成功测试禁止发布和回滚。
- [ ] T049 [US3] 编写 `tests/e2e/prompt-release.spec.ts`，完成 P1 发布与回滚验收。

## 阶段 6：用户故事 4——外部调用 API（P1）

**目标**：为已发布 Prompt 提供可撤销、限流、可追踪的外部调用契约。

**独立验收**：签发凭证并调用已发布 Prompt，停用凭证后后续请求被拒绝。

- [ ] T050 [P] [US4] 创建 `db/migration/V006__ai_external_credentials.sql`，建立凭证摘要、前缀、过期和启停字段。
- [ ] T051 [US4] 实现 `ruoyi-modules/ai-openapi/application/ExternalCredentialService.java`，完成随机凭证生成、摘要校验、轮换和停用。
- [ ] T052 [US4] 实现 `ruoyi-modules/ai-openapi/infrastructure/ProjectApiKeyFilter.java`，校验 Bearer 凭证、项目范围、过期和限流。
- [ ] T053 [US4] 实现 `ruoyi-modules/ai-openapi/controller/PublishedInvokeController.java`，严格按 `contracts/openapi.yaml` 暴露 Prompt 调用接口。
- [ ] T054 [US4] 将工作流调用接口预留在 `PublishedInvokeController.java`，未实现资源返回统一未发布/不可用错误。
- [ ] T055 [P] [US4] 创建 `plus-ui/src/views/ai/openapi/` 凭证签发、首次展示、停用和调用文档页面。
- [ ] T056 [US4] 编写 `tests/integration/PublishedInvokeIT.java`，覆盖有效调用、草稿拒绝、无效/停用凭证、限流和 Trace ID。
- [ ] T057 [US4] 编写 `tests/contract/openapi-contract-test.ts`，校验 `contracts/openapi.yaml` 与实际响应结构一致。

## 阶段 7：用户故事 6——执行记录与审计（P2）

**目标**：集中查询 Prompt/API 执行、用量、费用、错误和安全审计。

**独立验收**：产生成功、失败和发布变更记录，并按条件查询且不泄露敏感内容。

- [ ] T058 [P] [US6] 创建 `db/migration/V007__ai_execution_audit.sql`，建立执行、节点执行和审计事件表及查询索引。
- [ ] T059 [US6] 实现 `ruoyi-modules/ai-execution/application/ExecutionQueryService.java`，按项目、资源、版本、状态、时间和 Trace ID 查询。
- [ ] T060 [US6] 实现 `ruoyi-modules/ai-audit/application/AuditQueryService.java` 和事务后事件监听器，保证发布、回滚、凭证和权限变更可审计。
- [ ] T061 [US6] 实现 `ruoyi-modules/ai-execution/controller/ExecutionController.java` 和 `ruoyi-modules/ai-audit/controller/AuditController.java`。
- [ ] T062 [P] [US6] 实现 `plus-ui/src/views/ai/execution/` 运行列表、运行详情、节点详情和 `plus-ui/src/views/ai/audit/` 审计查询页面。
- [ ] T063 [US6] 编写 `tests/integration/ExecutionAuditIT.java`，验证项目过滤、留存模式、敏感字段脱敏和审计不可修改。
- [ ] T064 [US6] 编写 `tests/e2e/execution-audit.spec.ts`，完成运行追踪和审计查询验收。

## 阶段 8：用户故事 5——低代码工作流（P2）

**目标**：实现六种受限节点、画布、DAG 校验、异步执行、节点状态和工作流发布。

**独立验收**：构建 `START -> LLM -> END` 并运行；非法环路、断连和引用无法发布。

- [ ] T065 [P] [US5] 创建 `db/migration/V008__ai_workflow.sql`，建立工作流主表和版本表。
- [ ] T066 [P] [US5] 创建 `ruoyi-modules/ai-workflow/domain/WorkflowDefinition.java`、节点定义、边定义和节点类型白名单。
- [ ] T067 [US5] 实现 `ruoyi-modules/ai-workflow/domain/WorkflowValidator.java`，校验开始/结束、引用、环路、可达性和映射。
- [ ] T068 [P] [US5] 创建 `ruoyi-modules/ai-workflow/application/WorkflowNodeExecutor.java` 端口及 START、END、PROMPT、LLM、CONDITION、HTTP 执行器。
- [ ] T069 [US5] 实现 `ruoyi-modules/ai-workflow/application/WorkflowExecutionService.java`，使用受控线程池按拓扑顺序执行并持久化节点状态。
- [ ] T070 [US5] 实现 HTTP 节点 SSRF 防护、目标白名单、超时和响应大小限制，文件位于 `ruoyi-modules/ai-workflow/infrastructure/SafeHttpClient.java`。
- [ ] T071 [US5] 实现 `ruoyi-modules/ai-workflow/controller/WorkflowController.java` 和校验、调试、发布、运行接口。
- [ ] T072 [P] [US5] 实现 `plus-ui/src/views/ai/workflow/` Vue Flow 画布、节点面板、配置面板和运行高亮。
- [ ] T073 [US5] 编写 `tests/unit/WorkflowValidatorTest.java` 和 `WorkflowStateMachineTest.java`。
- [ ] T074 [US5] 编写 `tests/integration/WorkflowExecutionIT.java`，验证拓扑执行、条件分支、失败/跳过节点和执行记录。
- [ ] T075 [US5] 编写 `tests/e2e/workflow.spec.ts`，完成画布保存、校验、调试、发布和运行详情验收。

## 阶段 9：收尾与跨模块质量

**目标**：完成安全、性能、文档、部署和全链路验收。

- [ ] T076 [P] 为所有 AI Controller 补充 OpenAPI 注解并生成 `docs/api/openapi.json`。
- [ ] T077 [P] 为所有模块补充 `README.md`，说明责任边界、公开服务和禁止依赖。
- [ ] T078 [P] 添加 `tests/architecture/NoCrossModuleMapperAccessTest.java` 和 Maven/前端格式校验。
- [ ] T079 [P] 添加模型并发、执行列表分页、限流和 SSE 断开场景的性能测试脚本 `tests/performance/`。
- [ ] T080 [P] 完成密钥扫描、SSRF 测试、日志脱敏测试和依赖漏洞检查，记录 `docs/verification/security-review.md`。
- [ ] T081 执行 `quickstart.md` 的四个验收场景并记录 `docs/verification/mvp-acceptance.md`。
- [ ] T082 更新根目录 `README.md`、`SPEC.md` 和部署文档，说明模块化开发顺序和本地启动方式。
- [ ] T083 创建 `deploy/compose/README.md`，说明环境变量、模型服务配置、数据初始化和备份恢复。

## 依赖与执行顺序

### 阶段依赖

- 阶段 1 无依赖，可立即开始。
- 阶段 2 依赖阶段 1，并阻塞所有用户故事。
- US1 依赖阶段 2，是其他故事的权限基础。
- US2 依赖阶段 2 和 US1 的项目访问服务。
- US3 依赖 US2 的 Prompt 版本和执行记录。
- US4 依赖 US3 的发布指针和 US2 的 Prompt 调用服务。
- US6 依赖 US2、US3、US4 写入的执行和审计事件。
- US5 依赖 US1、US2、US3 和 US6 的执行记录能力。
- 阶段 9 依赖所有计划交付的用户故事。

### 可并行工作

- 阶段 1 的 T002-T005 可并行。
- 阶段 2 的 T008-T010、T013-T014 可并行。
- US1 的实体、Mapper 和前端页面可并行，服务实现完成后再接入控制器。
- US2 的模型基础、Prompt 表结构和渲染器可并行。
- US3 的版本生命周期、比较服务和前端版本面板可并行。
- US6 的数据库、查询服务和前端页面可并行，但必须等待执行/审计写入点明确。
- US5 的节点定义、校验器和画布基础可并行，执行器依赖端口定义。

## MVP 策略

1. 完成阶段 1 和阶段 2，确认基座可运行且权限/安全基础通过。
2. 完成 US1，作为第一个可独立演示的 MVP 增量。
3. 完成 US2、US3、US4，形成 Prompt 创建、测试、发布和 API 调用闭环。
4. 完成 US6 后再交付 US5，确保工作流从第一天就可追踪。
5. 最后执行阶段 9 的安全、性能和 quickstart 验收。

每个阶段完成后必须提交一次，并在 `docs/verification/` 留下验收证据。
