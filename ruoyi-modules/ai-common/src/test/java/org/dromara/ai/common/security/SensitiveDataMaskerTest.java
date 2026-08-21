package org.dromara.ai.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
class SensitiveDataMaskerTest {

    @Test
    void masksBearerAndSecretFields() {
        String source = "Authorization: Bearer sk-secret-value apiKey=another-secret";

        String masked = SensitiveDataMasker.mask(source);

        assertFalse(masked.contains("sk-secret-value"));
        assertFalse(masked.contains("another-secret"));
        assertEquals("Authorization: Bearer *** apiKey=***", masked);
    }
}
