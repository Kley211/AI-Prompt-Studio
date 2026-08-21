package org.dromara.ai.common.project;

import java.util.Objects;

public record ProjectResourceScope(long projectId) {

    public ProjectResourceScope {
        if (projectId <= 0) {
            throw new IllegalArgumentException("projectId must be positive");
        }
    }

    public void requireSameProject(Long resourceProjectId) {
        if (!Objects.equals(projectId, resourceProjectId)) {
            throw new org.dromara.ai.common.error.AiPlatformException(
                org.dromara.ai.common.error.AiErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
