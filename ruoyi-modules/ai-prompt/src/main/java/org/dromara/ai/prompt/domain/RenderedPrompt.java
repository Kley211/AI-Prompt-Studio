package org.dromara.ai.prompt.domain;

import java.util.Map;

public record RenderedPrompt(String systemPrompt, String userPrompt, Map<String, Object> variables) {
    public RenderedPrompt {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
