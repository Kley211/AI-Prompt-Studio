# MVP 快速验收指南

本指南用于验证从项目创建到 Prompt/工作流调用的完整业务闭环。它不是部署手册，也不替代
实现任务中的自动化测试。

## 前置条件

- Java 21、Node.js 20+、pnpm 10+、Docker Desktop 和 Git。
- 一个可用的 OpenAI Compatible 模型服务及测试凭证。
- 项目已按实施计划初始化 RuoYi-Vue-Plus 6.x。

## 启动依赖

```powershell
docker compose -f deploy/compose/docker-compose.yml up -d postgres redis
```

设置本地配置中的数据库、Redis、应用加密主密钥和模型服务地址。不得把真实密钥提交到 Git。

## 启动应用

```powershell
./mvnw clean verify
./mvnw -pl ruoyi-admin spring-boot:run
cd plus-ui
pnpm install
pnpm dev
```

## 验收场景

### 场景 1：项目隔离

1. 使用管理员创建用户 Alice、Bob 和项目 A、项目 B。
2. 只将 Alice 加入项目 A，将 Bob 加入项目 B。
3. 使用 Alice 查询项目 B 的项目、Prompt 和执行记录。
4. 预期：返回未授权，不泄露资源是否存在的敏感详情。

### 场景 2：Prompt 生命周期

1. 为项目 A 配置一个 OpenAI Compatible 模型并启用。
2. Alice 创建包含必填变量 `topic` 的 Prompt。
3. 不填写 `topic` 进行测试，预期不发生模型调用并返回校验错误。
4. 填写 `topic` 进行测试，预期返回结果、Token、耗时、费用和 executionId。
5. 将版本发布；再次编辑，预期产生草稿而不是修改已发布版本。

### 场景 3：外部调用

1. 项目管理员签发外部凭证，记录首次展示的原始值。
2. 使用资源 code 和变量调用已发布 Prompt。
3. 预期返回结果和 executionId，且响应中无供应商密钥。
4. 停用凭证后再次调用，预期返回未授权并留下失败审计/执行记录。

### 场景 4：工作流

1. 创建 `START -> LLM -> END` 工作流并绑定已测试 Prompt/模型。
2. 保存、校验并运行，预期三个节点按顺序成功。
3. 增加环路或删除 END 节点重新校验，预期发布被阻止并显示具体原因。
4. 查看运行详情，预期能看到每个节点状态及总 executionId/traceId。

## 自动化验证入口

```powershell
./mvnw test
pnpm --dir plus-ui test:unit
pnpm --dir plus-ui test:e2e
```

实现阶段必须将上述场景映射到 `tests/integration` 和 `tests/e2e`，并在每个里程碑的任务中
记录实际执行命令和结果。
