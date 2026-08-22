package org.dromara.ai.project.domain;

import org.dromara.ai.common.project.ProjectAction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ProjectRoleTest {
    @Test void viewerCanOnlyView() {
        assertTrue(ProjectRole.VIEWER.allows(ProjectAction.VIEW));
        assertFalse(ProjectRole.VIEWER.allows(ProjectAction.EDIT));
        assertFalse(ProjectRole.VIEWER.allows(ProjectAction.PUBLISH));
        assertFalse(ProjectRole.VIEWER.allows(ProjectAction.MANAGE_MEMBERS));
    }
    @Test void publisherCanPublishButCannotManageMembers() {
        assertTrue(ProjectRole.PUBLISHER.allows(ProjectAction.PUBLISH));
        assertFalse(ProjectRole.PUBLISHER.allows(ProjectAction.MANAGE_MEMBERS));
    }
}
