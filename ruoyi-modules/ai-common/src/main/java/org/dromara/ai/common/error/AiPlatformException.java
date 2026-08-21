package org.dromara.ai.common.error;

import java.io.Serial;

public final class AiPlatformException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final AiErrorCode errorCode;

    public AiPlatformException(AiErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public AiPlatformException(AiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiErrorCode errorCode() {
        return errorCode;
    }
}
