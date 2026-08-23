package org.dromara.ai.model.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@EnableConfigurationProperties(ModelSecurityProperties.class)
public class SecretCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final ModelSecurityProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public SecretCipher(ModelSecurityProperties properties) {
        this(properties, new SecureRandom());
    }

    SecretCipher(ModelSecurityProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public EncryptedSecret encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new SecretCipherException("待加密凭证不能为空");
        }
        String version = properties.getEncryptionKeyVersion();
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, plaintext.getBytes(StandardCharsets.UTF_8), iv, keyFor(version));
        byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        return new EncryptedSecret(Base64.getEncoder().encodeToString(payload), version);
    }

    public String decrypt(String ciphertext, String keyVersion) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new SecretCipherException("密文不能为空");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            if (payload.length <= IV_LENGTH) {
                throw new SecretCipherException("密文格式无效");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            ByteBuffer.wrap(payload).get(iv).get(encrypted);
            return new String(crypt(Cipher.DECRYPT_MODE, encrypted, iv, keyFor(keyVersion)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new SecretCipherException("密文格式无效", exception);
        }
    }

    private byte[] crypt(int mode, byte[] input, byte[] iv, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new SecretCipherException("凭证加解密失败", exception);
        }
    }

    private byte[] keyFor(String version) {
        String encodedKey = version != null && version.equals(properties.getEncryptionKeyVersion())
            ? properties.getEncryptionKey()
            : properties.getDecryptionKeys().get(version);
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new SecretCipherException("未配置密钥版本: " + version);
        }
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) {
                throw new SecretCipherException("AES-256 主密钥必须是 32 字节的 Base64 值");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new SecretCipherException("主密钥不是有效的 Base64 值", exception);
        }
    }

    public record EncryptedSecret(String ciphertext, String keyVersion) {
    }
}
