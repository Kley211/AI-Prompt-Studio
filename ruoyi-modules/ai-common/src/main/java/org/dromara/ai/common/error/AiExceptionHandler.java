package org.dromara.ai.common.error;

import org.dromara.ai.common.trace.TraceIdContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(AiPlatformException.class)
    public ResponseEntity<AiErrorResponse> handle(AiPlatformException exception) {
        AiErrorCode error = exception.errorCode();
        AiErrorResponse response = new AiErrorResponse(
            error.httpStatus(), error.code(), exception.getMessage(), TraceIdContext.current(), Instant.now());
        return ResponseEntity.status(error.httpStatus()).body(response);
    }
}
