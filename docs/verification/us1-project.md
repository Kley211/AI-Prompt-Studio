# US1 项目隔离验收

验证日期：2026-08-22

## 验收范围

- 项目表和成员表已在本地 PostgreSQL 创建，含项目编码唯一约束、成员联合唯一约束、状态约束和查询索引。
- 创建项目会自动写入 OWNER 成员；项目编码创建后不可修改。
- 项目成员角色支持 OWNER、ADMIN、DEVELOPER、PUBLISHER、VIEWER。
- VIEWER 不能编辑；PUBLISHER 可以发布但不能管理成员；OWNER 转移前不能降级或移除。
- 项目接口同时使用 `@SaCheckPermission` 系统菜单权限和 `ProjectAccessService` 项目级权限。
- 项目列表按当前用户的 ACTIVE 成员关系过滤，资源详情先进行项目权限判断。

## 已执行的自动化验证

```text
命令：.\mvnw.cmd -pl ruoyi-modules/ai-project test -Pdev -Dmaven.test.skip=false -Dcheckstyle.skip=true
结果：通过，执行 5 项测试，其中 ProjectAuthorizationIT 3 项，失败 0，错误 0。

命令：.\mvnw.cmd -pl tests/architecture test -Pdev -Dmaven.test.skip=false -Dcheckstyle.skip=true
结果：通过，执行 1 项模块边界测试，失败 0，错误 0。

命令：.\mvnw.cmd -pl ruoyi-admin -am package -DskipTests -Dcheckstyle.skip=true
结果：通过，37 个 Reactor 模块全部构建成功。

命令：corepack pnpm lint
结果：通过，oxlint 未报告问题。

命令：corepack pnpm build:dev
结果：通过，Vite 完成 3353 个模块转换并生成项目管理页面构建产物。
```

## E2E 状态

`tests/e2e/project-access.spec.ts` 已覆盖项目创建、成员添加，以及使用 VIEWER 账号时编辑和归档按钮不可见的场景。仓库当前尚未配置 `@playwright/test`、浏览器运行环境和专用验收账号，因此本次未执行浏览器 E2E，不将其计入自动化通过结果。

手工验收路径：登录后打开“AI Prompt Studio -> 项目管理”，创建项目，进入成员管理抽屉，添加成员并验证角色限制。
