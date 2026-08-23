package org.dromara.ai.execution.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 单次 Prompt/工作流执行的领域快照。持久化由后续应用层负责。
 */
public final class ExecutionRecord {
    private final String executionId;
    private final String traceId;
    private final long projectId;
    private final ExecutionResourceType resourceType;
    private final long resourceId;
    private final long versionId;
    private final ExecutionInitiatorType initiatorType;
    private final Instant createdAt;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private ExecutionUsage usage;
    private BigDecimal estimatedCost;
    private String sanitizedError;

    private ExecutionRecord(String executionId, String traceId, long projectId, ExecutionResourceType resourceType,
                            long resourceId, long versionId, ExecutionInitiatorType initiatorType) {
        this.executionId = requireText(executionId, "executionId");
        this.traceId = requireText(traceId, "traceId");
        if (projectId <= 0 || resourceId <= 0 || versionId <= 0) throw new IllegalArgumentException("资源标识必须为正数");
        this.projectId = projectId; this.resourceType = Objects.requireNonNull(resourceType); this.resourceId = resourceId;
        this.versionId = versionId; this.initiatorType = Objects.requireNonNull(initiatorType);
        this.createdAt = Instant.now(); this.status = ExecutionStatus.PENDING;
    }

    public static ExecutionRecord create(long projectId, ExecutionResourceType resourceType, long resourceId,
                                         long versionId, ExecutionInitiatorType initiatorType) {
        return new ExecutionRecord(UUID.randomUUID().toString(), UUID.randomUUID().toString(), projectId,
            resourceType, resourceId, versionId, initiatorType);
    }

    public void start() {
        requireStatus(ExecutionStatus.PENDING);
        startedAt = Instant.now(); status = ExecutionStatus.RUNNING;
    }

    public void succeed(ExecutionUsage usage, BigDecimal estimatedCost) {
        requireStatus(ExecutionStatus.RUNNING); this.usage = Objects.requireNonNull(usage);
        this.estimatedCost = nonNegativeCost(estimatedCost); finish(ExecutionStatus.SUCCEEDED);
    }

    public void fail(String sanitizedError, ExecutionUsage usage, BigDecimal estimatedCost) {
        requireStatus(ExecutionStatus.RUNNING); this.sanitizedError = requireText(sanitizedError, "错误详情");
        this.usage = usage; this.estimatedCost = nonNegativeCost(estimatedCost); finish(ExecutionStatus.FAILED);
    }

    public void cancel() { requireStatus(ExecutionStatus.PENDING, ExecutionStatus.RUNNING); finish(ExecutionStatus.CANCELLED); }

    public Duration duration() {
        Instant end = finishedAt == null ? Instant.now() : finishedAt;
        Instant begin = startedAt == null ? createdAt : startedAt;
        return Duration.between(begin, end);
    }

    private void finish(ExecutionStatus next) { finishedAt = Instant.now(); status = next; }
    private void requireStatus(ExecutionStatus... expected) {
        for (ExecutionStatus item : expected) if (status == item) return;
        throw new IllegalStateException("执行状态不允许从 " + status + " 变更");
    }
    private static BigDecimal nonNegativeCost(BigDecimal value) {
        if (value != null && value.signum() < 0) throw new IllegalArgumentException("预估费用不能为负数");
        return value;
    }
    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + "不能为空");
        return value;
    }

    public String executionId() { return executionId; }
    public String traceId() { return traceId; }
    public long projectId() { return projectId; }
    public ExecutionResourceType resourceType() { return resourceType; }
    public long resourceId() { return resourceId; }
    public long versionId() { return versionId; }
    public ExecutionInitiatorType initiatorType() { return initiatorType; }
    public Instant createdAt() { return createdAt; }
    public ExecutionStatus status() { return status; }
    public Instant startedAt() { return startedAt; }
    public Instant finishedAt() { return finishedAt; }
    public ExecutionUsage usage() { return usage; }
    public BigDecimal estimatedCost() { return estimatedCost; }
    public String sanitizedError() { return sanitizedError; }
}
