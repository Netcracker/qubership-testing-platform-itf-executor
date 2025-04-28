DROP INDEX IF EXISTS mb_configuration_by_ei_session_parent_id;
commit;

delete from mb_configuration where type='ei_session';
commit;

ALTER TABLE mb_configuration DROP CONSTRAINT IF EXISTS fkd3sb0cwbxnypsa061pvm2g0ki;
commit;

ALTER TABLE mb_configuration DROP COLUMN IF EXISTS ei_session_parent_id;
commit;
