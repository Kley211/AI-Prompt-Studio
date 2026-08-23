package org.dromara.ai.execution.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 按模型配置的每百万 Token 价格计算预估费用。价格缺失时返回 null，表示无法估算。
 */
public final class ExecutionCostCalculator {
    private static final BigDecimal TOKENS_PER_MILLION = BigDecimal.valueOf(1_000_000L);
    private static final int SCALE = 8;

    private ExecutionCostCalculator() { }

    public static BigDecimal estimate(ExecutionUsage usage, BigDecimal inputPricePerMillion,
                                      BigDecimal outputPricePerMillion) {
        if (usage == null || inputPricePerMillion == null || outputPricePerMillion == null) {
            return null;
        }
        requireNonNegative(inputPricePerMillion, "输入价格");
        requireNonNegative(outputPricePerMillion, "输出价格");
        return BigDecimal.valueOf(usage.inputTokens()).multiply(inputPricePerMillion)
            .add(BigDecimal.valueOf(usage.outputTokens()).multiply(outputPricePerMillion))
            .divide(TOKENS_PER_MILLION, SCALE, RoundingMode.HALF_UP)
            .stripTrailingZeros();
    }

    private static void requireNonNegative(BigDecimal price, String name) {
        if (price.signum() < 0) {
            throw new IllegalArgumentException(name + "不能为负数");
        }
    }
}
