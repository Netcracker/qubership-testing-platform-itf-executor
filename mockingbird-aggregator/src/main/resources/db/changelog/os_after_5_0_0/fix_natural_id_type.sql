
ALTER TABLE mb_projects ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_configuration ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_folders ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_upgrade_history ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_templates ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_systems ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_step_container ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_steps ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_servers ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_parsing_rules ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_operations ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_message_param ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_message ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_interceptors ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_triggers ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_env ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_counter ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_instance ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;
