package org.dromara.ai.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.common.project.ProjectAccessService;
import org.dromara.ai.common.project.ProjectAction;
import org.dromara.ai.project.application.ProjectApplicationService;
import org.dromara.ai.project.application.ProjectMemberCommand;
import org.dromara.ai.project.domain.ProjectMember;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectApplicationService service;
    private final ProjectAccessService accessService;

    @SaCheckPermission("ai:project:member")
    @GetMapping
    public R<List<ProjectMember>> list(@PathVariable long projectId) {
        require(projectId); return R.ok(service.members(projectId));
    }

    @SaCheckPermission("ai:project:member")
    @PostMapping
    public R<ProjectMember> add(@PathVariable long projectId, @Valid @RequestBody ProjectMemberCommand.Add command) {
        require(projectId); return R.ok(service.addMember(projectId, command));
    }

    @SaCheckPermission("ai:project:member")
    @PutMapping("/{userId}")
    public R<Void> changeRole(@PathVariable long projectId, @PathVariable long userId,
                              @Valid @RequestBody ProjectMemberCommand.ChangeRole command) {
        require(projectId); service.changeRole(projectId, userId, command.role()); return R.ok();
    }

    @SaCheckPermission("ai:project:member")
    @DeleteMapping("/{userId}")
    public R<Void> remove(@PathVariable long projectId, @PathVariable long userId) {
        require(projectId); service.removeMember(projectId, userId); return R.ok();
    }

    @SaCheckPermission("ai:project:member")
    @PostMapping("/transfer-owner")
    public R<Void> transferOwner(@PathVariable long projectId,
                                 @Valid @RequestBody ProjectMemberCommand.TransferOwnership command) {
        require(projectId);
        service.transferOwnership(projectId, LoginHelper.getUserId(), command.userId());
        return R.ok();
    }

    private void require(long projectId) {
        accessService.requireAccess(LoginHelper.getUserId(), projectId, ProjectAction.MANAGE_MEMBERS);
    }
}
