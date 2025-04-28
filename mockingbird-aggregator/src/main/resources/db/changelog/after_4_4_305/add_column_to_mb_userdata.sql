ALTER TABLE mb_userdata ADD COLUMN IF NOT EXISTS date_created timestamp default now();
commit;
ALTER TABLE mb_userdata ADD COLUMN IF NOT EXISTS date_modified timestamp default now();
commit;
