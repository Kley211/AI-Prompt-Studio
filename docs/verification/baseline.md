# 基线验证记录

## 2026-08-21

- 后端基线：`./mvnw.cmd package '-DskipTests' '-Dcheckstyle.skip=true'`
- 结果：通过，RuoYi-Vue-Plus 6.0.0 全部 Maven 模块 `BUILD SUCCESS`。
- 编译目标：Java release 21；执行环境为兼容 Java 21 release 的 JDK 23.0.2，后续开发仍要求使用 JDK 21。
- 前端基线：已接入官方 `JavaLionLi/plus-ui` 6.0.0 源码，待安装 pnpm 依赖后执行构建。
- 运行依赖：已提供 PostgreSQL 17、Redis 8 Compose 配置，待 Docker 环境执行健康检查。

## 当前限制

- 尚未执行登录冒烟测试，需在本地数据库初始化并启动 `ruoyi-admin` 后补充。
- 尚未执行前端依赖安装和生产构建。

## 阶段 2 进行中（2026-08-21）

- 新增 `ai-common` 模块，定义项目访问端口、分页对象、平台错误协议、Trace ID Filter 和敏感字段脱敏。
- 验证命令：`./mvnw.cmd -pl ruoyi-modules/ai-common -am test -Dmaven.test.skip=false -Dcheckstyle.skip=true`
- 结果：通过，`ai-common` 3 项单元测试通过。
