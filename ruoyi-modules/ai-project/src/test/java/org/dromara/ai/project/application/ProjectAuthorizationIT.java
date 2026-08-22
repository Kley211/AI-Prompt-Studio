package org.dromara.ai.project.application;

import org.dromara.ai.common.project.ProjectAction;
import org.dromara.ai.project.domain.ProjectMember;
import org.dromara.ai.project.domain.ProjectRole;
import org.dromara.ai.project.infrastructure.ProjectMemberMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * US1 项目级授权集成测试，验证权限服务始终以当前有效成员关系为准。
 */
@Tag("dev")
class ProjectAuthorizationIT {
    @Test
    void userCannotReadAnotherProjectWithoutActiveMembership() {
        ProjectMemberMapper memberMapper = mock(ProjectMemberMapper.class);
        ProjectAccessServiceImpl accessService = new ProjectAccessServiceImpl(memberMapper);
        when(memberMapper.selectActiveMember(200L, 10L)).thenReturn(null);

        assertFalse(accessService.canAccess(10L, 200L, ProjectAction.VIEW));
    }

    @Test
    void viewerCanReadButCannotEditProject() {
        ProjectMemberMapper memberMapper = mock(ProjectMemberMapper.class);
        ProjectAccessServiceImpl accessService = new ProjectAccessServiceImpl(memberMapper);
        when(memberMapper.selectActiveMember(100L, 10L)).thenReturn(activeMember(ProjectRole.VIEWER));

        assertTrue(accessService.canAccess(10L, 100L, ProjectAction.VIEW));
        assertFalse(accessService.canAccess(10L, 100L, ProjectAction.EDIT));
    }

    @Test
    void removedMemberLosesAccessImmediately() {
        ProjectMemberMapper memberMapper = mock(ProjectMemberMapper.class);
        ProjectAccessServiceImpl accessService = new ProjectAccessServiceImpl(memberMapper);
        when(memberMapper.selectActiveMember(100L, 10L))
            .thenReturn(activeMember(ProjectRole.DEVELOPER))
            .thenReturn(null);

        assertTrue(accessService.canAccess(10L, 100L, ProjectAction.EDIT));
        assertFalse(accessService.canAccess(10L, 100L, ProjectAction.VIEW));
    }

    private ProjectMember activeMember(ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setRole(role);
        member.setStatus("ACTIVE");
        return member;
    }
}
