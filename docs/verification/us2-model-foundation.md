# US2 模型基础验收

验证日期：2026-08-22

## 本阶段完成

- 新增 `ai-model` Maven 模块，并接入 `ruoyi-admin` 单体启动模块。
- 创建供应商、模型、模型凭证和项目模型授权四张 PostgreSQL 表。
- 模型凭证使用 AES-256-GCM 加密，随机 IV 不重复，密钥版本随密文保存。
- 支持通过 `ai-platform.security.decryption-keys` 保留旧密钥版本，用于密钥轮换后的解密。
- 建立 `ChatModelGateway` 及请求、响应、流式事件和统一错误契约，业务模块不依赖供应商 SDK。
- 增加 `ai-model` 基础设施私有边界 ArchUnit 规则。

## 自动化验证

```text
命令：.\mvnw.cmd -pl ruoyi-modules/ai-model test -Pdev -Dmaven.test.skip=false -Dcheckstyle.skip=true
结果：通过，SecretCipherTest 3 项通过。

命令：.\mvnw.cmd -pl tests/architecture test -Pdev -Dmaven.test.skip=false -Dcheckstyle.skip=true
结果：通过，ModuleBoundaryTest 2 项通过。

命令：.\mvnw.cmd -pl ruoyi-admin -am package -DskipTests -Dcheckstyle.skip=true
结果：通过，38 个 Reactor 模块全部构建成功。
```

数据库迁移 `V004__ai_model.sql` 已在本地 PostgreSQL 执行，确认创建：
`ai_model_provider`、`ai_model_credential`、`ai_model`、`ai_project_model`。

## 当前边界

本阶段尚未提供模型管理 Controller、供应商真实 HTTP 调用或前端模型页面；这些内容属于 T031、T030、T037 和 T038，完成后才进入可视化操作验收。
