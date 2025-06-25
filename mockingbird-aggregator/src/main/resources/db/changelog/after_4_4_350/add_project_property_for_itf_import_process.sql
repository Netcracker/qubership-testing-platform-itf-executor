insert into mb_project_settings(project_id, prop_short_name, prop_value)
values(unnest(array(select id from mb_projects)), 'enable.event.triggers.activation.after.import', 'true')
on conflict do nothing;

commit;