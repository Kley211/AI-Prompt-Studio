package org.dromara.ai.prompt.domain;

public record PromptValidationError(String path, String code, String message) {
}
