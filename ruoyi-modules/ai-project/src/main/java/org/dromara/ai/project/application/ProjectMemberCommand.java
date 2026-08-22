package org.dromara.ai.project.application;

import jakarta.validation.constraints.NotNull;
import org.dromara.ai.project.domain.ProjectRole;

public final class ProjectMemberCommand {
    private ProjectMemberCommand() { }
    public record Add(@NotNull Long userId, @NotNull ProjectRole role) { }
    public record ChangeRole(@NotNull ProjectRole role) { }
    public record TransferOwnership(@NotNull Long userId) { }
}
