# ai-common

跨 AI 业务模块共享的最小公共层，负责错误协议、Trace ID、日志脱敏和项目访问端口。

允许业务模块依赖本模块。禁止本模块依赖 `ai-project`、`ai-model`、`ai-prompt` 等具体业务模块，
也禁止在本模块中定义业务 Entity 或 Mapper。
