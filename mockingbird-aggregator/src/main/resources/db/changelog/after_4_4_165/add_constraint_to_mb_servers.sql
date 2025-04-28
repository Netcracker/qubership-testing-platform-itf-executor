update mb_servers srv
SET project_id = ( select project_id from mb_folders where id = srv.parent_id )
where project_id is null;
commit;
ALTER TABLE mb_servers ALTER COLUMN project_id SET NOT NULL;