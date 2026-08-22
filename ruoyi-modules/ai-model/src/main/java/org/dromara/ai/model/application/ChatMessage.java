package org.dromara.ai.model.application;

public record ChatMessage(Role role, String content) {
    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
}
