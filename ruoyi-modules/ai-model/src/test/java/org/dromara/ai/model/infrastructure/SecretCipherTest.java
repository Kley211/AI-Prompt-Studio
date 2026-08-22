package org.dromara.ai.model.infrastructure;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class SecretCipherTest {
    private static final String KEY_V1 = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsWithRandomIvAndDecryptsWithRecordedVersion() {
        SecretCipher cipher = new SecretCipher(properties(KEY_V1, "v1", Map.of()));

        SecretCipher.EncryptedSecret first = cipher.encrypt("sk-local-test");
        SecretCipher.EncryptedSecret second = cipher.encrypt("sk-local-test");

        assertNotEquals(first.ciphertext(), second.ciphertext());
        assertEquals("v1", first.keyVersion());
        assertEquals("sk-local-test", cipher.decrypt(first.ciphertext(), first.keyVersion()));
    }

    @Test
    void decryptsCredentialEncryptedByPreviousKeyVersion() {
        SecretCipher oldCipher = new SecretCipher(properties(KEY_V1, "v1", Map.of()));
        SecretCipher.EncryptedSecret encrypted = oldCipher.encrypt("previous-secret");
        byte[] rotatedKey = new byte[32];
        rotatedKey[0] = 1;
        String keyV2 = Base64.getEncoder().encodeToString(rotatedKey);
        SecretCipher rotatedCipher = new SecretCipher(properties(keyV2, "v2", Map.of("v1", KEY_V1)));

        assertEquals("previous-secret", rotatedCipher.decrypt(encrypted.ciphertext(), "v1"));
    }

    @Test
    void rejectsMissingKeyVersion() {
        SecretCipher cipher = new SecretCipher(properties(KEY_V1, "v1", Map.of()));

        assertThrows(SecretCipherException.class, () -> cipher.decrypt("invalid", "v0"));
    }

    private ModelSecurityProperties properties(String key, String version, Map<String, String> oldKeys) {
        ModelSecurityProperties properties = new ModelSecurityProperties();
        properties.setEncryptionKey(key);
        properties.setEncryptionKeyVersion(version);
        properties.setDecryptionKeys(oldKeys);
        return properties;
    }
}
