package org.dromara.ai.prompt.domain;

import tools.jackson.databind.JsonNode;

public enum PromptVariableType {
    STRING {
        @Override
        boolean accepts(JsonNode value) {
            return value.isTextual();
        }
    },
    NUMBER {
        @Override
        boolean accepts(JsonNode value) {
            return value.isNumber();
        }
    },
    INTEGER {
        @Override
        boolean accepts(JsonNode value) {
            return value.isIntegralNumber();
        }
    },
    BOOLEAN {
        @Override
        boolean accepts(JsonNode value) {
            return value.isBoolean();
        }
    },
    OBJECT {
        @Override
        boolean accepts(JsonNode value) {
            return value.isObject();
        }
    },
    ARRAY {
        @Override
        boolean accepts(JsonNode value) {
            return value.isArray();
        }
    };

    abstract boolean accepts(JsonNode value);
}
