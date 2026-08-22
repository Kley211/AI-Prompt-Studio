package org.dromara.ai.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.common.project.ProjectAccessService;
import org.dromara.ai.common.project.ProjectAction;
import org.dromara.ai.project.application.ProjectApplicationService;
import org.dromara.ai.project.application.ProjectCommand;
import org.dromara.ai.project.domain.Project;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/projects")
public class ProjectController {
    private final ProjectApplicationService service;
    private final ProjectAccessService accessService;

    @SaCheckPermission("ai:project:list")
    @GetMapping
    public R<List<Project>> list() { return R.ok(service.listForUser(LoginHelper.getUserId())); }

    @SaCheckPermission("ai:project:list")
    @GetMapping("/{id}")
    public R<Project> get(@PathVariable long id) {
        accessService.requireAccess(LoginHelper.getUserId(), id, ProjectAction.VIEW); return R.ok(service.get(id));
    }

    @SaCheckPermission("ai:project:add")
    @PostMapping
    public R<Project> create(@Valid @RequestBody ProjectCommand.Create command) {
        return R.ok(service.create(command, LoginHelper.getUserId()));
    }

    @SaCheckPermission("ai:project:edit")
    @PutMapping("/{id}")
    public R<Project> update(@PathVariable long id, @Valid @RequestBody ProjectCommand.Update command) {
        accessService.requireAccess(LoginHelper.getUserId(), id, ProjectAction.EDIT); return R.ok(service.update(id, command));
    }

    @SaCheckPermission("ai:project:edit")
    @PostMapping("/{id}/archive")
    public R<Void> archive(@PathVariable long id) {
        accessService.requireAccess(LoginHelper.getUserId(), id, ProjectAction.EDIT); service.archive(id); return R.ok();
    }
}
