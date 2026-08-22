package org.dromara.ai.model.application;

public class ModelGatewayException extends RuntimeException {
    private final ModelGatewayError error;
    private final Integer providerStatus;

    public ModelGatewayException(ModelGatewayError error, String message) {
        this(error, message, null, null);
    }

    public ModelGatewayException(ModelGatewayError error, String message, Integer providerStatus, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.providerStatus = providerStatus;
    }

    public ModelGatewayError error() {
        return error;
    }

    public Integer providerStatus() {
        return providerStatus;
    }

    public boolean retryable() {
        return error.retryable();
    }
}
