package org.dromara.ai.model.application;

public enum ModelGatewayError {
    INVALID_REQUEST(false),
    AUTHENTICATION_FAILED(false),
    MODEL_NOT_FOUND(false),
    RATE_LIMITED(true),
    TIMEOUT(true),
    PROVIDER_UNAVAILABLE(true),
    INVALID_RESPONSE(false);

    private final boolean retryable;

    ModelGatewayError(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
