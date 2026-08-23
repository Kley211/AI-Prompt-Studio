create table if not exists ai_prompt
(
    id                      bigint primary key,
    project_id              bigint not null references ai_project(id),
    code                    varchar(64) not null,
    name                    varchar(128) not null,
    description             varchar(500),
    current_draft_version_id bigint,
    current_published_version_id bigint,
    status                  varchar(20) not null default 'ACTIVE',
    create_dept             bigint,
    create_by               bigint,
    create_time             timestamp not null default now(),
    update_by               bigint,
    update_time             timestamp,
    del_flag                bigint not null default 0,
    constraint uk_ai_prompt_project_code unique (project_id, code),
    constraint uk_ai_prompt_id_project unique (id, project_id),
    constraint ck_ai_prompt_status check (status in ('ACTIVE', 'ARCHIVED'))
);

create table if not exists ai_prompt_version
(
    id                  bigint primary key,
    project_id          bigint not null,
    prompt_id           bigint not null,
    version_no          integer not null,
    system_template     text,
    user_template       text not null,
    variables           jsonb not null default '[]'::jsonb,
    input_schema        jsonb,
    output_schema       jsonb,
    model_id            bigint not null references ai_model(id),
    model_parameters    jsonb not null default '{}'::jsonb,
    status              varchar(20) not null default 'DRAFT',
    change_note         varchar(500),
    successful_test     boolean not null default false,
    lock_version        integer not null default 0,
    create_dept         bigint,
    create_by           bigint,
    create_time         timestamp not null default now(),
    update_by           bigint,
    update_time         timestamp,
    del_flag            bigint not null default 0,
    constraint fk_ai_prompt_version_prompt
        foreign key (prompt_id, project_id) references ai_prompt(id, project_id),
    constraint uk_ai_prompt_version_no unique (prompt_id, version_no),
    constraint uk_ai_prompt_version_id_prompt unique (id, prompt_id),
    constraint ck_ai_prompt_version_no check (version_no > 0),
    constraint ck_ai_prompt_version_status
        check (status in ('DRAFT', 'TESTABLE', 'PUBLISHED', 'ARCHIVED')),
    constraint ck_ai_prompt_variables_array check (jsonb_typeof(variables) = 'array'),
    constraint ck_ai_prompt_input_schema_object
        check (input_schema is null or jsonb_typeof(input_schema) = 'object'),
    constraint ck_ai_prompt_output_schema_object
        check (output_schema is null or jsonb_typeof(output_schema) = 'object'),
    constraint ck_ai_prompt_model_parameters_object check (jsonb_typeof(model_parameters) = 'object'),
    constraint ck_ai_prompt_lock_version check (lock_version >= 0)
);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_ai_prompt_current_draft') then
        alter table ai_prompt
            add constraint fk_ai_prompt_current_draft
                foreign key (current_draft_version_id, id)
                references ai_prompt_version(id, prompt_id)
                deferrable initially deferred;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'fk_ai_prompt_current_published') then
        alter table ai_prompt
            add constraint fk_ai_prompt_current_published
                foreign key (current_published_version_id, id)
                references ai_prompt_version(id, prompt_id)
                deferrable initially deferred;
    end if;
end;
$$;

create index if not exists idx_ai_prompt_project_status
    on ai_prompt(project_id, status, create_time desc);
create index if not exists idx_ai_prompt_current_published
    on ai_prompt(current_published_version_id)
    where current_published_version_id is not null;
create index if not exists idx_ai_prompt_version_prompt_status
    on ai_prompt_version(prompt_id, status, version_no desc);
create index if not exists idx_ai_prompt_version_project
    on ai_prompt_version(project_id, create_time desc);
create index if not exists idx_ai_prompt_version_model
    on ai_prompt_version(model_id, status);
create index if not exists idx_ai_prompt_variables_gin
    on ai_prompt_version using gin(variables);

create or replace function prevent_published_prompt_version_mutation()
returns trigger
language plpgsql
as $$
begin
    if old.status in ('PUBLISHED', 'ARCHIVED') then
        if tg_op = 'DELETE' then
            raise exception 'published or archived prompt version % is immutable', old.id
                using errcode = '55000';
        end if;
        if (old.status = 'PUBLISHED' and new.status not in ('PUBLISHED', 'ARCHIVED'))
            or (old.status = 'ARCHIVED' and new.status <> 'ARCHIVED')
            or (to_jsonb(new) - array['status', 'update_by', 'update_time'])
                is distinct from
               (to_jsonb(old) - array['status', 'update_by', 'update_time']) then
            raise exception 'published or archived prompt version % content is immutable', old.id
                using errcode = '55000';
        end if;
    end if;
    if tg_op = 'DELETE' then
        return old;
    end if;
    return new;
end;
$$;

drop trigger if exists trg_ai_prompt_version_immutable on ai_prompt_version;
create trigger trg_ai_prompt_version_immutable
before update or delete on ai_prompt_version
for each row execute function prevent_published_prompt_version_mutation();

comment on table ai_prompt is 'Prompt 稳定资源及当前草稿、发布版本指针';
comment on table ai_prompt_version is 'Prompt 模板、变量、Schema 与模型设置的版本快照';
comment on column ai_prompt_version.variables is '变量定义数组，包含名称、类型、必填、描述和默认值';
comment on column ai_prompt_version.successful_test is '当前版本是否至少完成过一次成功测试';
