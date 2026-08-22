package org.dromara.ai.project.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.dromara.ai.project.domain.RetentionMode;

public final class ProjectCommand {
    private ProjectCommand() { }

    public record Create(
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{2,63}") String code,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        @NotNull RetentionMode retentionMode) { }

    public record Update(@NotBlank @Size(max = 128) String name,
                         @Size(max = 500) String description,
                         @NotNull RetentionMode retentionMode) { }
}
