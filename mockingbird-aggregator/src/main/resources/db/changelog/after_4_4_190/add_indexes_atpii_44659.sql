create index if not exists config_by_server_system_type
on mb_configuration (parent_out_server_id, system_id, type_name);
commit;

create index if not exists template_by_project_type_name on mb_templates (project_id, type, name);
commit;