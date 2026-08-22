package org.dromara.ai.project.domain;

public enum ProjectRole {
    OWNER, ADMIN, DEVELOPER, PUBLISHER, VIEWER;

    public boolean allows(org.dromara.ai.common.project.ProjectAction action) {
        return switch (action) {
            case VIEW -> true;
            case EDIT -> this != VIEWER;
            case PUBLISH -> this == OWNER || this == ADMIN || this == PUBLISHER;
            case MANAGE_MEMBERS -> this == OWNER || this == ADMIN;
        };
    }
}
