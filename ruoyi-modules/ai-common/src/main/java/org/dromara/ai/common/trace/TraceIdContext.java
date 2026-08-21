package org.dromara.ai.common.trace;

import org.slf4j.MDC;

public final class TraceIdContext {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private TraceIdContext() {
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
