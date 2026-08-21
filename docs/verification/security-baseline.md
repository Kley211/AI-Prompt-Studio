# 安全基线验证

验证日期：2026-08-21

- 新增和修改的 AI 平台配置只包含占位符，真实密钥通过环境变量注入。
- `application-local.yml`、`.env`、日志和构建产物已由 `.gitignore` 排除。
- 外部 Bearer/API Key 仅保留摘要或脱敏值，公共模块提供统一脱敏器。
- 菜单权限迁移使用固定 ID 与幂等插入，不包含凭证、密码或 Token。

检查命令：

```text
git diff --check
rg -n --glob '!script/sql/**' --glob '!target/**' '(sk-[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{20,}|Bearer [A-Za-z0-9._-]{20,})'
```

结果：通过。命令未发现新增真实 API Key、密码或生产配置。
