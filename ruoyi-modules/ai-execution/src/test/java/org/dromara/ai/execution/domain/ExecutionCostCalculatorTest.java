package org.dromara.ai.execution.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ExecutionCostCalculatorTest {
    @Test
    void calculatesInputAndOutputPricePerMillionTokens() {
        BigDecimal cost = ExecutionCostCalculator.estimate(new ExecutionUsage(1_000_000, 500_000),
            BigDecimal.valueOf(2), BigDecimal.valueOf(4));
        assertEquals(0, cost.compareTo(BigDecimal.valueOf(4)));
    }

    @Test
    void returnsUnknownWhenAnyPriceIsMissing() {
        assertNull(ExecutionCostCalculator.estimate(new ExecutionUsage(1, 1), BigDecimal.ONE, null));
    }

    @Test
    void rejectsNegativeUsageAndPrice() {
        assertThrows(IllegalArgumentException.class, () -> new ExecutionUsage(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> ExecutionCostCalculator.estimate(
            new ExecutionUsage(1, 1), BigDecimal.valueOf(-1), BigDecimal.ONE));
    }
}
