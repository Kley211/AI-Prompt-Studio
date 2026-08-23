package org.dromara.ai.model.application;

import org.dromara.ai.model.domain.ModelStatus;

public record ModelCredentialView(
    Long id,
    Long providerId,
    String name,
    String secretPrefix,
    ModelStatus status
) {
}
