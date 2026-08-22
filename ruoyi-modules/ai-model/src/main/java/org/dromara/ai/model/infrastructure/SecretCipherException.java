package org.dromara.ai.model.infrastructure;

public class SecretCipherException extends RuntimeException {
    public SecretCipherException(String message) {
        super(message);
    }

    public SecretCipherException(String message, Throwable cause) {
        super(message, cause);
    }
}
