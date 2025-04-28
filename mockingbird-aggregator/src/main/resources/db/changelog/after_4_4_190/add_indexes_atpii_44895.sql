create index if not exists config_by_type_template_typename
on mb_configuration (type, parent_template_id, type_name);
commit;
