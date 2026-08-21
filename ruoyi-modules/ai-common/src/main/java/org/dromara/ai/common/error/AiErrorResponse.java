package org.dromara.ai.common.error;

import java.time.Instant;

public record AiErrorResponse(
    int status,
    String code,
    String message,
    String traceId,
    Instant timestamp
) {
}
