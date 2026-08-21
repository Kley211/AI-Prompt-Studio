package org.dromara.ai.common.project;

public interface ProjectAccessService {

    boolean canAccess(long userId, long projectId, ProjectAction action);

    default void requireAccess(long userId, long projectId, ProjectAction action) {
        if (!canAccess(userId, projectId, action)) {
            throw new org.dromara.ai.common.error.AiPlatformException(
                org.dromara.ai.common.error.AiErrorCode.PROJECT_ACCESS_DENIED);
        }
    }
}
