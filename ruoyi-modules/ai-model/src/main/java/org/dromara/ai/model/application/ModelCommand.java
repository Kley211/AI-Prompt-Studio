package org.dromara.ai.model.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.dromara.ai.model.domain.ModelCapabilities;
import org.dromara.ai.model.domain.ModelStatus;
import org.dromara.ai.model.domain.ModelType;
import org.dromara.ai.model.domain.ProviderProtocol;

import java.math.BigDecimal;

public final class ModelCommand {
    private ModelCommand() {
    }

    public record CreateProvider(
        @NotBlank @Size(max = 128) String name,
        @NotNull ProviderProtocol protocol,
        @NotBlank @Size(max = 500) String baseUrl,
        @Size(max = 500) String description
    ) {
    }

    public record UpdateProvider(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 500) String baseUrl,
        @Size(max = 500) String description,
        @NotNull ModelStatus status
    ) {
    }

    public record SaveCredential(
        @NotBlank @Size(max = 128) String name,
        @NotBlank String secret
    ) {
    }

    public record CreateModel(
        @NotNull Long providerId,
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(max = 128) String displayName,
        @NotNull ModelType modelType,
        @NotNull ModelCapabilities capabilities,
        @Positive Integer contextWindow,
        @DecimalMin("0") BigDecimal inputPrice,
        @DecimalMin("0") BigDecimal outputPrice
    ) {
    }

    public record UpdateModel(
        @NotBlank @Size(max = 128) String displayName,
        @NotNull ModelCapabilities capabilities,
        @Positive Integer contextWindow,
        @DecimalMin("0") BigDecimal inputPrice,
        @DecimalMin("0") BigDecimal outputPrice,
        @NotNull ModelStatus status
    ) {
    }

    public record GrantProjectModel(
        @NotNull Long modelId,
        @Size(max = 128) String alias
    ) {
    }
}
