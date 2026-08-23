package org.dromara.ai.execution.domain;

/** 执行记录生命周期门面，确保所有状态变化遵守领域规则。 */
public final class ExecutionRecorder {
    private ExecutionRecorder() { }

    public static ExecutionRecord begin(long projectId, ExecutionResourceType resourceType, long resourceId,
                                        long versionId, ExecutionInitiatorType initiatorType) {
        return ExecutionRecord.create(projectId, resourceType, resourceId, versionId, initiatorType);
    }
}
