package org.dromara.ai.project.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.common.error.AiErrorCode;
import org.dromara.ai.common.error.AiPlatformException;
import org.dromara.ai.project.domain.*;
import org.dromara.ai.project.infrastructure.ProjectMapper;
import org.dromara.ai.project.infrastructure.ProjectMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;

    public List<Project> listForUser(long userId) {
        List<Long> ids = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getUserId, userId).eq(ProjectMember::getStatus, "ACTIVE"))
            .stream().map(ProjectMember::getProjectId).toList();
        return ids.isEmpty() ? List.of() : projectMapper.selectByIds(ids);
    }

    public Project get(long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new AiPlatformException(AiErrorCode.RESOURCE_NOT_FOUND);
        return project;
    }

    @Transactional
    public Project create(ProjectCommand.Create command, long ownerId) {
        if (projectMapper.selectCount(Wrappers.<Project>lambdaQuery().eq(Project::getCode, command.code())) > 0) {
            throw new AiPlatformException(AiErrorCode.CONFLICT, "项目编码已存在");
        }
        Project project = new Project();
        project.setCode(command.code()); project.setName(command.name()); project.setDescription(command.description());
        project.setRetentionMode(command.retentionMode()); project.setStatus(ProjectStatus.ACTIVE); project.setOwnerId(ownerId);
        projectMapper.insert(project);
        ProjectMember owner = member(project.getId(), ownerId, ProjectRole.OWNER);
        memberMapper.insert(owner);
        return project;
    }

    @Transactional
    public Project update(long id, ProjectCommand.Update command) {
        Project project = get(id);
        if (project.getStatus() == ProjectStatus.ARCHIVED) throw new AiPlatformException(AiErrorCode.CONFLICT, "归档项目不可修改");
        project.setName(command.name()); project.setDescription(command.description()); project.setRetentionMode(command.retentionMode());
        projectMapper.updateById(project);
        return project;
    }

    @Transactional
    public void archive(long id) {
        Project project = get(id); project.setStatus(ProjectStatus.ARCHIVED); projectMapper.updateById(project);
    }

    public List<ProjectMember> members(long projectId) {
        return memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, "ACTIVE"));
    }

    @Transactional
    public ProjectMember addMember(long projectId, ProjectMemberCommand.Add command) {
        get(projectId);
        ProjectMember existing = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
            .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, command.userId()));
        if (existing != null) {
            existing.setRole(command.role()); existing.setStatus("ACTIVE"); memberMapper.updateById(existing); return existing;
        }
        ProjectMember member = member(projectId, command.userId(), command.role()); memberMapper.insert(member); return member;
    }

    @Transactional
    public void changeRole(long projectId, long userId, ProjectRole role) {
        ProjectMember member = requireMember(projectId, userId);
        if (member.getRole() == ProjectRole.OWNER && role != ProjectRole.OWNER) throw new AiPlatformException(AiErrorCode.CONFLICT, "请先转移项目所有权");
        member.setRole(role); memberMapper.updateById(member);
    }

    @Transactional
    public void removeMember(long projectId, long userId) {
        ProjectMember member = requireMember(projectId, userId);
        if (member.getRole() == ProjectRole.OWNER) throw new AiPlatformException(AiErrorCode.CONFLICT, "项目所有者不可移除");
        member.setStatus("REMOVED"); memberMapper.updateById(member);
    }

    @Transactional
    public void transferOwnership(long projectId, long currentOwnerId, long newOwnerId) {
        Project project = get(projectId);
        if (!project.getOwnerId().equals(currentOwnerId)) {
            throw new AiPlatformException(AiErrorCode.PROJECT_ACCESS_DENIED);
        }
        ProjectMember currentOwner = requireMember(projectId, currentOwnerId);
        ProjectMember newOwner = requireMember(projectId, newOwnerId);
        currentOwner.setRole(ProjectRole.ADMIN);
        newOwner.setRole(ProjectRole.OWNER);
        memberMapper.updateById(currentOwner);
        memberMapper.updateById(newOwner);
        project.setOwnerId(newOwnerId);
        projectMapper.updateById(project);
    }

    private ProjectMember requireMember(long projectId, long userId) {
        ProjectMember member = memberMapper.selectActiveMember(projectId, userId);
        if (member == null) throw new AiPlatformException(AiErrorCode.RESOURCE_NOT_FOUND); return member;
    }

    private static ProjectMember member(long projectId, long userId, ProjectRole role) {
        ProjectMember member = new ProjectMember(); member.setProjectId(projectId); member.setUserId(userId);
        member.setRole(role); member.setStatus("ACTIVE"); return member;
    }
}
