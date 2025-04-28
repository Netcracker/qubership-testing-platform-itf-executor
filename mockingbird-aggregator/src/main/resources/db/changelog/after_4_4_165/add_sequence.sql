-- 'serial' sequence is created to generate REPORTING OBJECTS' ids in the EXECUTOR database
-- The key factors are:
--  1. These objects are not copied/moved, exported/imported, so their ids uniqueness between
--  different databases is not needed. So, instead of sophisticated getid(), we can use simple sequence.
--  2. Ids are necessary on the execution side, for two purposes:
--      1. Logging (records to Graylog/console and business ids to Grafana) - TcContext id is needed
--      2. For all types of objects: if objects are received on the reporting side without ids,
--         their ids are determined on the fly. It works, but requires extra queries for parent objects.
--         So, to decrease heavy load of reporting database, it's better to generate ids before reporting,
--         where all objects' relationships are known.

-- Sequence usage:
--  - Simple usage:
--      select nextval('serial');
--      select * from serial;
--      select setval('serial', 999999999999);
--
--  - Usage in executor application:
--      select nextval('serial') from generate_series(1,500);

create sequence if not exists serial start 1 increment 1 maxvalue 999999999999 cycle;
commit;