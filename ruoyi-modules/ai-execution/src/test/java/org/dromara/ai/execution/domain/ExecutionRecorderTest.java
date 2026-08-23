package org.dromara.ai.execution.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ExecutionRecorderTest {
    @Test
    void createsUniqueTraceableExecutionAndCompletes() {
        ExecutionRecord record = ExecutionRecorder.begin(1, ExecutionResourceType.PROMPT, 2, 3, ExecutionInitiatorType.USER);
        assertEquals(ExecutionStatus.PENDING, record.status());
        assertNotEquals(record.executionId(), record.traceId());
        record.start();
        record.succeed(new ExecutionUsage(120, 80), BigDecimal.valueOf(0.001));
        assertEquals(ExecutionStatus.SUCCEEDED, record.status());
        assertEquals(200, record.usage().totalTokens());
        assertNotNull(record.finishedAt());
        assertFalse(record.duration().isNegative());
    }

    @Test
    void rejectsIllegalTransitionsAndKeepsTerminalState() {
        ExecutionRecord record = ExecutionRecorder.begin(1, ExecutionResourceType.WORKFLOW, 2, 3, ExecutionInitiatorType.API);
        assertThrows(IllegalStateException.class, () -> record.succeed(new ExecutionUsage(1, 1), null));
        record.start(); record.cancel();
        assertEquals(ExecutionStatus.CANCELLED, record.status());
        assertThrows(IllegalStateException.class, record::start);
    }

    @Test
    void storesOnlySanitizedFailureDetail() {
        ExecutionRecord record = ExecutionRecorder.begin(1, ExecutionResourceType.PROMPT, 2, 3, ExecutionInitiatorType.WORKFLOW);
        record.start(); record.fail("供应商请求超时", new ExecutionUsage(10, 0), null);
        assertEquals(ExecutionStatus.FAILED, record.status());
        assertEquals("供应商请求超时", record.sanitizedError());
        assertNull(record.estimatedCost());
    }
}
