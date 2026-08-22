package org.dromara.ai.project.application;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.common.project.ProjectAccessService;
import org.dromara.ai.common.project.ProjectAction;
import org.dromara.ai.project.domain.ProjectMember;
import org.dromara.ai.project.infrastructure.ProjectMemberMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessServiceImpl implements ProjectAccessService {
    private final ProjectMemberMapper memberMapper;

    @Override
    public boolean canAccess(long userId, long projectId, ProjectAction action) {
        ProjectMember member = memberMapper.selectActiveMember(projectId, userId);
        return member != null && member.getRole().allows(action);
    }
}
