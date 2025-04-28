alter table mb_steps add column if not exists condition_parameters text NULL;
commit;

alter table mb_triggers add column if not exists condition_parameters text NULL;
commit;

update mb_steps st
    set condition_parameters = (select condition_parameters from mb_condition_props where step_id = st.id)
    where id in (select distinct step_id from mb_condition_props where step_id is not null);
commit;

update mb_triggers trg
    set condition_parameters = (select condition_parameters from mb_condition_props where trigger_id = trg.id)
    where id in (select distinct trigger_id from mb_condition_props where trigger_id is not null);
commit;

alter table mb_condition_props drop constraint if exists fk42rkh13qxtoa2f0rqnrhlcy09;
commit;

alter table mb_condition_props drop constraint if exists fk96t0o93ubr1ubysysqtulxgfh;
commit;
