-- AI 平台菜单与系统权限初始化。
-- 使用固定 ID 和幂等插入，允许重复启动或重新执行迁移而不产生重复数据。

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu,
     ext, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (1761400000000003000, 'AI Prompt Studio', 0, 20, 'ai', null, '', 'N', 'Y', 'M', '0', '0', '', 'magic-wand', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, 'AI 能力治理平台'),
    (1761400000000003001, '项目管理', 1761400000000003000, 1, 'project', 'ai/project/index', '', 'N', 'Y', 'C', '0', '0', 'ai:project:list', 'tree', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, 'AI 项目与成员管理'),
    (1761400000000003002, '模型配置', 1761400000000003000, 2, 'model', 'ai/model/index', '', 'N', 'Y', 'C', '0', '0', 'ai:model:list', 'monitor', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '模型供应商与授权'),
    (1761400000000003003, 'Prompt 管理', 1761400000000003000, 3, 'prompt', 'ai/prompt/index', '', 'N', 'Y', 'C', '0', '0', 'ai:prompt:list', 'edit', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, 'Prompt 模板与版本'),
    (1761400000000003004, '工作流', 1761400000000003000, 4, 'workflow', 'ai/workflow/index', '', 'N', 'Y', 'C', '0', '0', 'ai:workflow:list', 'guide', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '低代码工作流'),
    (1761400000000003005, '执行记录', 1761400000000003000, 5, 'execution', 'ai/execution/index', '', 'N', 'Y', 'C', '0', '0', 'ai:execution:list', 'log', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '执行与用量记录'),
    (1761400000000003006, '审计日志', 1761400000000003000, 6, 'audit', 'ai/audit/index', '', 'N', 'Y', 'C', '0', '0', 'ai:audit:list', 'log', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '安全审计查询')
on conflict (menu_id) do nothing;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu,
     ext, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (1761400000000003101, '项目新增', 1761400000000003001, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:project:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003102, '项目修改', 1761400000000003001, 2, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:project:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003103, '项目成员管理', 1761400000000003001, 3, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:project:member', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003104, '模型配置', 1761400000000003002, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:model:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003105, 'Prompt 编辑', 1761400000000003003, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:prompt:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003106, 'Prompt 发布', 1761400000000003003, 2, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:prompt:publish', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003107, '工作流编辑', 1761400000000003004, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:workflow:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003108, '执行查询', 1761400000000003005, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:execution:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, ''),
    (1761400000000003109, '审计查询', 1761400000000003006, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'ai:audit:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select 1761300000000000001, menu_id
from sys_menu
where menu_id between 1761400000000003000 and 1761400000000003199
on conflict (role_id, menu_id) do nothing;
