create table if not exists ai_project
(
    id              bigint primary key,
    code            varchar(64) not null,
    name            varchar(128) not null,
    description     varchar(500),
    status          varchar(20) not null default 'ACTIVE',
    retention_mode  varchar(20) not null default 'MASKED',
    owner_id        bigint not null,
    create_dept     bigint,
    create_by       bigint,
    create_time     timestamp not null default now(),
    update_by       bigint,
    update_time     timestamp,
    del_flag        bigint not null default 0,
    constraint uk_ai_project_code unique (code),
    constraint ck_ai_project_status check (status in ('ACTIVE', 'ARCHIVED')),
    constraint ck_ai_project_retention check (retention_mode in ('FULL', 'MASKED', 'METADATA_ONLY'))
);

create table if not exists ai_project_member
(
    id          bigint primary key,
    project_id  bigint not null references ai_project(id),
    user_id     bigint not null,
    role        varchar(20) not null,
    status      varchar(20) not null default 'ACTIVE',
    create_dept bigint,
    create_by   bigint,
    create_time timestamp not null default now(),
    update_by   bigint,
    update_time timestamp,
    del_flag    bigint not null default 0,
    constraint uk_ai_project_member unique (project_id, user_id),
    constraint ck_ai_project_member_role check (role in ('OWNER', 'ADMIN', 'DEVELOPER', 'PUBLISHER', 'VIEWER')),
    constraint ck_ai_project_member_status check (status in ('ACTIVE', 'REMOVED'))
);

create index if not exists idx_ai_project_owner on ai_project(owner_id);
create index if not exists idx_ai_project_member_user on ai_project_member(user_id, status);
create index if not exists idx_ai_project_member_project on ai_project_member(project_id, status);
