package org.dromara.ai.common.project;

import org.dromara.ai.common.error.AiPlatformException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class ProjectAccessServiceTest {

    @Test
    void deniesUnauthorizedProjectAction() {
        ProjectAccessService accessService = (userId, projectId, action) -> false;

        assertThrows(AiPlatformException.class,
            () -> accessService.requireAccess(1L, 101L, ProjectAction.EDIT));
    }

    @Test
    void permitsAuthorizedProjectAction() {
        ProjectAccessService accessService = (userId, projectId, action) -> true;

        assertDoesNotThrow(() -> accessService.requireAccess(1L, 101L, ProjectAction.VIEW));
    }
}
