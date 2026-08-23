package org.dromara.ai.prompt.domain;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prompt 输入输出 Schema 值对象，支持 MVP 所需的 JSON Schema 类型与对象约束子集。
 */
public final class PromptSchema {
    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "object", "array", "string", "number", "integer", "boolean", "null");

    private final JsonNode schema;
    private final JsonMapper jsonMapper;

    private PromptSchema(JsonNode schema, JsonMapper jsonMapper) {
        this.schema = schema;
        this.jsonMapper = jsonMapper;
        validateDefinition(schema, "$schema");
    }

    public static PromptSchema parse(String schemaJson, JsonMapper jsonMapper) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return empty(jsonMapper);
        }
        JsonNode schema = jsonMapper.readTree(schemaJson);
        if (!schema.isObject()) {
            throw new IllegalArgumentException("Schema 必须是 JSON 对象");
        }
        return new PromptSchema(schema, jsonMapper);
    }

    public static PromptSchema empty(JsonMapper jsonMapper) {
        return new PromptSchema(jsonMapper.createObjectNode(), jsonMapper);
    }

    public List<PromptValidationError> validate(Object value) {
        if (schema.isEmpty()) {
            return List.of();
        }
        List<PromptValidationError> errors = new ArrayList<>();
        validateValue(schema, jsonMapper.valueToTree(value), "$", errors);
        return List.copyOf(errors);
    }

    public void requireValid(Object value) {
        List<PromptValidationError> errors = validate(value);
        if (!errors.isEmpty()) {
            throw new PromptValidationException(errors);
        }
    }

    public String toJson() {
        return jsonMapper.writeValueAsString(schema);
    }

    private void validateDefinition(JsonNode node, String path) {
        if (!node.isObject()) {
            throw new IllegalArgumentException(path + " 必须是对象");
        }
        JsonNode type = node.path("type");
        if (!type.isMissingNode() && (!type.isTextual() || !SUPPORTED_TYPES.contains(type.asText()))) {
            throw new IllegalArgumentException(path + ".type 不受支持");
        }
        JsonNode required = node.path("required");
        if (!required.isMissingNode()) {
            if (!required.isArray()) {
                throw new IllegalArgumentException(path + ".required 必须是数组");
            }
            Set<String> names = new HashSet<>();
            for (JsonNode name : required) {
                if (!name.isTextual() || !names.add(name.asText())) {
                    throw new IllegalArgumentException(path + ".required 包含无效或重复字段");
                }
            }
        }
        JsonNode properties = node.path("properties");
        if (!properties.isMissingNode()) {
            if (!properties.isObject()) {
                throw new IllegalArgumentException(path + ".properties 必须是对象");
            }
            Iterator<Map.Entry<String, JsonNode>> fields = properties.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                validateDefinition(field.getValue(), path + ".properties." + field.getKey());
            }
        }
        JsonNode items = node.path("items");
        if (!items.isMissingNode()) {
            validateDefinition(items, path + ".items");
        }
        JsonNode additionalProperties = node.path("additionalProperties");
        if (!additionalProperties.isMissingNode() && !additionalProperties.isBoolean()) {
            throw new IllegalArgumentException(path + ".additionalProperties 必须是布尔值");
        }
    }

    private void validateValue(JsonNode definition, JsonNode value, String path,
                               List<PromptValidationError> errors) {
        String type = definition.path("type").isTextual() ? definition.path("type").asText() : null;
        if (type != null && !accepts(type, value)) {
            errors.add(new PromptValidationError(path, "TYPE_MISMATCH",
                path + " 必须是 " + type));
            return;
        }
        if (value.isObject()) {
            validateObject(definition, value, path, errors);
        }
        if (value.isArray() && definition.path("items").isObject()) {
            for (int index = 0; index < value.size(); index++) {
                validateValue(definition.path("items"), value.get(index), path + "[" + index + "]", errors);
            }
        }
    }

    private void validateObject(JsonNode definition, JsonNode value, String path,
                                List<PromptValidationError> errors) {
        JsonNode required = definition.path("required");
        if (required.isArray()) {
            for (JsonNode name : required) {
                if (!value.has(name.asText()) || value.path(name.asText()).isNull()) {
                    errors.add(new PromptValidationError(path + "." + name.asText(), "REQUIRED",
                        "缺少必填字段 " + path + "." + name.asText()));
                }
            }
        }
        JsonNode properties = definition.path("properties");
        if (properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (value.has(field.getKey()) && !value.path(field.getKey()).isNull()) {
                    validateValue(field.getValue(), value.path(field.getKey()), path + "." + field.getKey(), errors);
                }
            }
            if (definition.path("additionalProperties").isBoolean()
                && !definition.path("additionalProperties").asBoolean()) {
                Iterator<String> inputNames = value.propertyNames().iterator();
                while (inputNames.hasNext()) {
                    String inputName = inputNames.next();
                    if (!properties.has(inputName)) {
                        errors.add(new PromptValidationError(path + "." + inputName, "ADDITIONAL_PROPERTY",
                            "不允许字段 " + path + "." + inputName));
                    }
                }
            }
        }
    }

    private boolean accepts(String type, JsonNode value) {
        return switch (type) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "number" -> value.isNumber();
            case "integer" -> value.isIntegralNumber();
            case "boolean" -> value.isBoolean();
            case "null" -> value.isNull();
            default -> false;
        };
    }
}
