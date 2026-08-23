package org.dromara.ai.prompt.domain;

import java.util.List;

public class PromptValidationException extends RuntimeException {
    private final List<PromptValidationError> errors;

    public PromptValidationException(List<PromptValidationError> errors) {
        super(errors == null || errors.isEmpty() ? "Prompt 输入校验失败" : errors.getFirst().message());
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public List<PromptValidationError> errors() {
        return errors;
    }
}
