package org.dromara.ai.prompt.domain;

import java.util.regex.Pattern;

public record PromptVariable(
    String name,
    PromptVariableType type,
    boolean required,
    String description,
    Object defaultValue
) {
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,63}");

    public PromptVariable {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("变量名必须以字母开头，且只能包含字母、数字和下划线");
        }
        if (type == null) {
            throw new IllegalArgumentException("变量类型不能为空");
        }
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
}
