package org.dromara.ai.model.application;

public record ChatUsage(int inputTokens, int outputTokens) {
    public static ChatUsage unknown() {
        return new ChatUsage(0, 0);
    }
}
