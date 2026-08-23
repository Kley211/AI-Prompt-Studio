package org.dromara.ai.prompt.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class PromptTemplateRendererTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer(jsonMapper);

    @Test
    void rendersDeclaredVariablesAndDefaults() {
        RenderedPrompt result = renderer.render(
            "你是 {{role}}",
            "请介绍 {{topic}}，标签：{{tags}}",
            List.of(
                new PromptVariable("role", PromptVariableType.STRING, true, null, "助手"),
                new PromptVariable("topic", PromptVariableType.STRING, true, null, null),
                new PromptVariable("tags", PromptVariableType.ARRAY, false, null, List.of("AI", "Java"))
            ),
            Map.of("topic", "Prompt 平台"),
            PromptSchema.empty(jsonMapper)
        );

        assertEquals("你是 助手", result.systemPrompt());
        assertEquals("请介绍 Prompt 平台，标签：[\"AI\",\"Java\"]", result.userPrompt());
    }

    @Test
    void rejectsMissingRequiredVariableBeforeRendering() {
        PromptValidationException exception = assertThrows(PromptValidationException.class, () -> renderer.render(
            null,
            "Hello {{name}}",
            List.of(new PromptVariable("name", PromptVariableType.STRING, true, null, null)),
            Map.of(),
            PromptSchema.empty(jsonMapper)
        ));

        assertEquals("REQUIRED", exception.errors().getFirst().code());
    }

    @Test
    void rejectsWrongVariableType() {
        PromptValidationException exception = assertThrows(PromptValidationException.class, () -> renderer.render(
            null,
            "Count {{count}}",
            List.of(new PromptVariable("count", PromptVariableType.INTEGER, true, null, null)),
            Map.of("count", "1"),
            PromptSchema.empty(jsonMapper)
        ));

        assertEquals("TYPE_MISMATCH", exception.errors().getFirst().code());
    }

    @Test
    void rejectsUndeclaredPlaceholder() {
        PromptValidationException exception = assertThrows(PromptValidationException.class, () -> renderer.render(
            null, "Hello {{unknown}}", List.of(), Map.of(), PromptSchema.empty(jsonMapper)));

        assertEquals("UNDECLARED_VARIABLE", exception.errors().getFirst().code());
    }

    @Test
    void rejectsUndeclaredInput() {
        PromptValidationException exception = assertThrows(PromptValidationException.class, () -> renderer.render(
            null, "Hello", List.of(), Map.of("unexpected", true), PromptSchema.empty(jsonMapper)));

        assertEquals("UNDECLARED_INPUT", exception.errors().getFirst().code());
    }

    @Test
    void doesNotEvaluatePlaceholdersInsideUserValues() {
        RenderedPrompt result = renderer.render(
            null,
            "Message: {{message}}",
            List.of(new PromptVariable("message", PromptVariableType.STRING, true, null, null)),
            Map.of("message", "{{other}}"),
            PromptSchema.empty(jsonMapper)
        );

        assertEquals("Message: {{other}}", result.userPrompt());
    }

    @Test
    void validatesNestedSchemaAndAdditionalProperties() {
        PromptSchema schema = PromptSchema.parse("""
            {
              "type":"object",
              "required":["user"],
              "additionalProperties":false,
              "properties":{
                "user":{
                  "type":"object",
                  "required":["age"],
                  "properties":{"age":{"type":"integer"}}
                }
              }
            }
            """, jsonMapper);

        List<PromptValidationError> errors = schema.validate(Map.of(
            "user", Map.of("age", "18"),
            "extra", true
        ));

        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(error -> error.path().equals("$.user.age")
            && error.code().equals("TYPE_MISMATCH")));
        assertTrue(errors.stream().anyMatch(error -> error.path().equals("$.extra")
            && error.code().equals("ADDITIONAL_PROPERTY")));
    }

    @Test
    void rejectsUnsupportedSchemaKeywordType() {
        assertThrows(IllegalArgumentException.class,
            () -> PromptSchema.parse("{\"type\":\"date\"}", jsonMapper));
    }
}
