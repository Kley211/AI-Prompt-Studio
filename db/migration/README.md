# 数据库迁移约定

迁移由 Flyway 按文件名顺序执行，文件命名为 `V<三位序号>__<英文描述>.sql`。

- 已执行迁移不可修改；修复使用新的版本迁移。
- 本项目不自动回滚生产迁移，回滚必须通过经过评审的反向迁移或备份恢复完成。
- `db/migration/` 保存平台业务迁移；RuoYi 系统原始表由上游初始化脚本管理。
- 本地启动前使用 `deploy/compose/docker-compose.yml` 提供 PostgreSQL 和 Redis。
