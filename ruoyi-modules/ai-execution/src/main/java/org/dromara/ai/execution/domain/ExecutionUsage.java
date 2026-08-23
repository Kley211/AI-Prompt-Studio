package org.dromara.ai.execution.domain;

public record ExecutionUsage(int inputTokens, int outputTokens) {
    public ExecutionUsage {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Token 用量不能为负数");
        }
    }

    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
