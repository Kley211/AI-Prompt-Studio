package org.dromara.ai.prompt.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dromara.ai.prompt.domain.PromptVariable;

import java.util.List;
import java.util.Map;

public final class PromptCommand {
    private PromptCommand() { }

    public record CreatePrompt(@NotBlank @Size(max = 64) String code,
                               @NotBlank @Size(max = 128) String name,
                               @Size(max = 500) String description) { }

    public record Draft(@NotBlank String userTemplate, String systemTemplate,
                        @NotNull List<PromptVariable> variables, String inputSchema,
                        String outputSchema, @NotNull Long modelId,
                        Map<String, Object> modelParameters,
                        @Size(max = 500) String changeNote) { }
}
