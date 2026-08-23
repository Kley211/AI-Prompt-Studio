package org.dromara.ai.execution.domain;

public enum ExecutionStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED, CANCELLED;

    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
