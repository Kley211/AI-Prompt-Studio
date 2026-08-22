package org.dromara.ai.model.application;

public record ChatModelResponse(
    String content,
    ChatUsage usage,
    String providerRequestId,
    String finishReason
) {
}
