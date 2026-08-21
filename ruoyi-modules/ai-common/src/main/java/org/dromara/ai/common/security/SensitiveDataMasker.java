package org.dromara.ai.common.security;

import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[a-z0-9._~+/-]+=*");
    private static final Pattern SECRET_FIELD = Pattern.compile(
        "(?i)(\\\"?(?:api[_-]?key|access[_-]?token|secret|password)\\\"?\\s*[:=]\\s*\\\"?)[^\\\",\\s}]+");

    private SensitiveDataMasker() {
    }

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = BEARER.matcher(value).replaceAll("$1***");
        return SECRET_FIELD.matcher(masked).replaceAll("$1***");
    }
}
