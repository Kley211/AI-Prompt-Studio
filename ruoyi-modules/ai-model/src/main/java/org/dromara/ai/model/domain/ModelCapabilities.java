package org.dromara.ai.model.domain;

public record ModelCapabilities(
    boolean streaming,
    boolean jsonMode,
    boolean toolCalling,
    boolean vision
) {
    public static ModelCapabilities chatDefaults() {
        return new ModelCapabilities(true, false, false, false);
    }
}
