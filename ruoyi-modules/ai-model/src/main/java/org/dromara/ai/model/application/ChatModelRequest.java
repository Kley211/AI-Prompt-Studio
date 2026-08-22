package org.dromara.ai.model.application;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ChatModelRequest(
    ModelRuntimeConfig runtime,
    List<ChatMessage> messages,
    Double temperature,
    Integer maxTokens,
    boolean jsonMode,
    Duration timeout,
    Map<String, Object> metadata
) {
    public ChatModelRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
    }
}
