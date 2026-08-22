package org.dromara.ai.model.application;

public record ChatStreamEvent(Type type, String content, ChatUsage usage, String finishReason) {
    public enum Type {
        CONTENT,
        COMPLETED
    }
}
