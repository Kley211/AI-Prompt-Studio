create table if not exists ai_model_provider
(
    id              bigint primary key,
    name            varchar(128) not null,
    protocol        varchar(32) not null,
    base_url        varchar(500) not null,
    description     varchar(500),
    status          varchar(20) not null default 'ACTIVE',
    create_dept     bigint,
    create_by       bigint,
    create_time     timestamp not null default now(),
    update_by       bigint,
    update_time     timestamp,
    del_flag        bigint not null default 0,
    constraint uk_ai_model_provider_name unique (name),
    constraint ck_ai_model_provider_protocol check (protocol in ('OPENAI_COMPATIBLE')),
    constraint ck_ai_model_provider_status check (status in ('ACTIVE', 'DISABLED'))
);

create table if not exists ai_model_credential
(
    id                bigint primary key,
    provider_id       bigint not null references ai_model_provider(id),
    name              varchar(128) not null,
    secret_prefix     varchar(16) not null,
    encrypted_secret  text not null,
    key_version       varchar(32) not null,
    status            varchar(20) not null default 'ACTIVE',
    create_dept       bigint,
    create_by         bigint,
    create_time       timestamp not null default now(),
    update_by         bigint,
    update_time       timestamp,
    del_flag          bigint not null default 0,
    constraint uk_ai_model_credential_name unique (provider_id, name),
    constraint ck_ai_model_credential_status check (status in ('ACTIVE', 'DISABLED'))
);

create table if not exists ai_model
(
    id                  bigint primary key,
    provider_id         bigint not null references ai_model_provider(id),
    code                varchar(128) not null,
    display_name        varchar(128) not null,
    model_type          varchar(32) not null,
    capabilities        jsonb not null default '{}'::jsonb,
    context_window      integer,
    input_price         numeric(18, 8),
    output_price        numeric(18, 8),
    status              varchar(20) not null default 'ACTIVE',
    create_dept         bigint,
    create_by           bigint,
    create_time         timestamp not null default now(),
    update_by           bigint,
    update_time         timestamp,
    del_flag            bigint not null default 0,
    constraint uk_ai_model_provider_code unique (provider_id, code),
    constraint ck_ai_model_type check (model_type in ('CHAT', 'EMBEDDING', 'RERANK')),
    constraint ck_ai_model_status check (status in ('ACTIVE', 'DISABLED')),
    constraint ck_ai_model_context_window check (context_window is null or context_window > 0),
    constraint ck_ai_model_input_price check (input_price is null or input_price >= 0),
    constraint ck_ai_model_output_price check (output_price is null or output_price >= 0)
);

create table if not exists ai_project_model
(
    id          bigint primary key,
    project_id  bigint not null references ai_project(id),
    model_id    bigint not null references ai_model(id),
    alias       varchar(128),
    status      varchar(20) not null default 'ACTIVE',
    create_dept bigint,
    create_by   bigint,
    create_time timestamp not null default now(),
    update_by   bigint,
    update_time timestamp,
    del_flag    bigint not null default 0,
    constraint uk_ai_project_model unique (project_id, model_id),
    constraint uk_ai_project_model_alias unique (project_id, alias),
    constraint ck_ai_project_model_status check (status in ('ACTIVE', 'DISABLED'))
);

create index if not exists idx_ai_model_provider_status on ai_model(provider_id, status);
create index if not exists idx_ai_model_credential_provider on ai_model_credential(provider_id, status);
create index if not exists idx_ai_project_model_project on ai_project_model(project_id, status);
create index if not exists idx_ai_project_model_model on ai_project_model(model_id, status);
