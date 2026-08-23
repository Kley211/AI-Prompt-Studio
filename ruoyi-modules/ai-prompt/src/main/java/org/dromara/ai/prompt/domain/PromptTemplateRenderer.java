package org.dromara.ai.prompt.domain;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptTemplateRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]{0,63})\\s*}}" );

    private final JsonMapper jsonMapper;

    public PromptTemplateRenderer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public RenderedPrompt render(String systemTemplate, String userTemplate,
                                 List<PromptVariable> variables, Map<String, Object> inputs,
                                 PromptSchema inputSchema) {
        if (userTemplate == null || userTemplate.isBlank()) {
            throw validation("$template", "EMPTY_TEMPLATE", "用户模板不能为空");
        }
        List<PromptVariable> definitions = variables == null ? List.of() : List.copyOf(variables);
        Map<String, Object> provided = inputs == null ? Map.of() : new LinkedHashMap<>(inputs);
        validateUniqueDefinitions(definitions);
        Map<String, Object> resolved = resolveVariables(definitions, provided);
        PromptSchema schema = inputSchema == null ? PromptSchema.empty(jsonMapper) : inputSchema;
        schema.requireValid(resolved);
        Set<String> declaredNames = new HashSet<>();
        definitions.forEach(variable -> declaredNames.add(variable.name()));
        String renderedSystem = renderTemplate(systemTemplate, resolved, declaredNames, "$systemTemplate");
        String renderedUser = renderTemplate(userTemplate, resolved, declaredNames, "$userTemplate");
        return new RenderedPrompt(renderedSystem, renderedUser, resolved);
    }

    private Map<String, Object> resolveVariables(List<PromptVariable> definitions, Map<String, Object> inputs) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        List<PromptValidationError> errors = new ArrayList<>();
        Set<String> declaredNames = new HashSet<>();
        definitions.forEach(variable -> declaredNames.add(variable.name()));
        for (String inputName : inputs.keySet()) {
            if (!declaredNames.contains(inputName)) {
                errors.add(new PromptValidationError("$." + inputName, "UNDECLARED_INPUT",
                    "输入包含未声明变量 " + inputName));
            }
        }
        for (PromptVariable variable : definitions) {
            Object value = inputs.containsKey(variable.name()) ? inputs.get(variable.name()) : variable.defaultValue();
            if (value == null) {
                if (variable.required()) {
                    errors.add(new PromptValidationError("$." + variable.name(), "REQUIRED",
                        "缺少必填变量 " + variable.name()));
                }
                continue;
            }
            JsonNode valueNode = jsonMapper.valueToTree(value);
            if (!variable.type().accepts(valueNode)) {
                errors.add(new PromptValidationError("$." + variable.name(), "TYPE_MISMATCH",
                    "变量 " + variable.name() + " 类型必须是 " + variable.type().name()));
                continue;
            }
            resolved.put(variable.name(), value);
        }
        if (!errors.isEmpty()) {
            throw new PromptValidationException(errors);
        }
        return resolved;
    }

    private String renderTemplate(String template, Map<String, Object> values,
                                  Set<String> declaredNames, String path) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer rendered = new StringBuffer();
        List<PromptValidationError> errors = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!declaredNames.contains(name)) {
                errors.add(new PromptValidationError(path, "UNDECLARED_VARIABLE",
                    "模板引用了未声明变量 " + name));
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            if (!values.containsKey(name)) {
                errors.add(new PromptValidationError("$." + name, "MISSING_VALUE",
                    "模板变量 " + name + " 没有可用值"));
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(renderValue(values.get(name))));
        }
        matcher.appendTail(rendered);
        if (!errors.isEmpty()) {
            throw new PromptValidationException(errors);
        }
        return rendered.toString();
    }

    private String renderValue(Object value) {
        JsonNode node = jsonMapper.valueToTree(value);
        return node.isTextual() ? node.asText() : jsonMapper.writeValueAsString(value);
    }

    private void validateUniqueDefinitions(List<PromptVariable> variables) {
        Map<String, PromptVariable> byName = new HashMap<>();
        for (PromptVariable variable : variables) {
            if (variable == null) {
                throw validation("$variables", "INVALID_DEFINITION", "变量定义不能为空");
            }
            if (byName.putIfAbsent(variable.name(), variable) != null) {
                throw validation("$." + variable.name(), "DUPLICATE_VARIABLE",
                    "变量定义重复: " + variable.name());
            }
            if (variable.hasDefaultValue()
                && !variable.type().accepts(jsonMapper.valueToTree(variable.defaultValue()))) {
                throw validation("$." + variable.name(), "INVALID_DEFAULT",
                    "变量默认值类型与定义不一致: " + variable.name());
            }
        }
    }

    private PromptValidationException validation(String path, String code, String message) {
        return new PromptValidationException(List.of(new PromptValidationError(path, code, message)));
    }
}
