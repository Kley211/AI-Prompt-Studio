package org.dromara.ai.model.application;

import java.net.URI;

public record ModelRuntimeConfig(
    Long providerId,
    Long modelId,
    URI baseUrl,
    String apiKey,
    String modelCode
) {
}
