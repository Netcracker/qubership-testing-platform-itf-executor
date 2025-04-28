--
-- PostgreSQL database dump
--

-- Dumped from database version 14.2
-- Dumped by pg_dump version 14.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: add_state(character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.add_state(old_env_state character varying, trigger_state character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN
case
        when old_env_state = '' or old_env_state = 'EMPTY' or old_env_state is null THEN
            return trigger_state;
when old_env_state = 'ACTIVE' THEN
            IF trigger_state = 'ERROR' THEN
                return 'ACTIVE_ERROR';
ELSE
                IF trigger_state = 'ACTIVE' THEN
                    return 'ACTIVE';
ELSE
                    return 'ACTIVE_PART';
END IF;
END IF;
when old_env_state = 'INACTIVE'  THEN
            IF trigger_state = 'ERROR' THEN
                return 'ERROR';
ELSE
                IF trigger_state = 'ACTIVE' THEN
                    return 'ACTIVE_PART';
ELSE
                    return 'INACTIVE';
END IF;
END IF;
when old_env_state = 'ACTIVE_PART' THEN
            IF trigger_state = 'ERROR' THEN
                return 'ACTIVE_ERROR';
ELSE
                return 'ACTIVE_PART';
END IF;
when old_env_state = 'ERROR' THEN
            IF trigger_state = 'ERROR' THEN
                return 'ERROR';
ELSE
                IF trigger_state = 'ACTIVE' THEN
                    return 'ACTIVE_ERROR';
ELSE
                    return 'ERROR';
END IF;
END IF;
else
            Return old_env_state;
END case;
END;
$$;


--
-- Name: compute_env_state(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.compute_env_state(env_id bigint) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
r mb_configuration%rowtype;
    env_state character varying:='EMPTY';
BEGIN
FOR r IN (select * from mb_configuration where id in (select * from get_all_env_triggers(env_id) ))
        LOOP
            env_state := add_state(env_state, r.trigger_state);
END LOOP;
Return env_state;
END;
$$;


--
-- Name: compute_env_state_before_env_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.compute_env_state_before_env_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    new.state := compute_env_state(new.id);
Return NEW;
END;
$$;


--
-- Name: create_project(uuid, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.create_project(project_uuid uuid, project_name character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
project_id BIGINT;
	system_root_folder_id BIGINT;
	chains_root_folder_id BIGINT;
	envs_root_folder_id BIGINT;
	servers_root_folder_id BIGINT;
BEGIN
	--Create project - Start
select getid() into project_id;
insert into mb_projects(id, version, name, uuid) values(project_id, 1, project_name, project_uuid);

select getid() into system_root_folder_id;
insert into mb_folders(id, version, type, name, project_id) values(system_root_folder_id, 1, 'systems', 'ROOT', project_id);

select getid() into chains_root_folder_id;
insert into mb_folders(id, version, type, name, project_id) values(chains_root_folder_id, 1, 'chains', 'ROOT', project_id);

select getid() into envs_root_folder_id;
insert into mb_folders(id, version, type, name, project_id) values(envs_root_folder_id, 1, 'envs', 'ROOT', project_id);

select getid() into servers_root_folder_id;
insert into mb_folders(id, version, type, name, project_id) values(servers_root_folder_id, 1, 'servers', 'ROOT', project_id);

update mb_projects
set systems_folder = system_root_folder_id,
    chains_folder = chains_root_folder_id,
    envs_folder = envs_root_folder_id,
    servers_folder = servers_root_folder_id
where id = project_id;
--Create project - End

--Set default project settings - Start
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'expression.var', 								'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'atp.reporting.mode', 							'sync');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.needToLogInAtp', 		'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'copyObjects.isSmart', 							'true');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'enable.report.adapter.atp', 					'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'atp.wsdl.path', 								'http://atp-service-address/solutions/atp/integration/jsp/AtpRamWebService?wsdl');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.link.KeyBasedLinkCollector.view.name', 	'Integration Sessions Log');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'scheduled.cleanup.enabled', 					'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'tcpdump.capturing.filter.default', 			null);
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'atp.reporting.wait.max', 						'30');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'ldap.url',										'ldap://domain-controller-service-address:9876');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.testData', 			'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.to.atp.default.context.enabled', 		'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'start.triggers.at.startup', 					'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'conditions.style.legacy', 						'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'bv.default.action', 							'CreateNewTestRun');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.createTcpDump', 		'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'enable.report.adapter.atp2', 					'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'atp.test.plan', 								null);
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.collectLogs',			'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'tcpdump.ni.default', 							null);
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'tc.timeout.fail.timeunit',						'MINUTES');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'tcpdump.packet.count.default',					'50');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'scheduled.cleanup.initialDelayMinutes', 		'20');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'scheduled.cleanup.hoursToDelete', 				'1');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'dataset.service.datasetFormat', 				'Itf');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.runBvCase', 			'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'atp.account.name',								null);
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'many.objects.ui.mode',							'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'context.format.prettyPrint', 					'true');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.execution.sender.thread.pool.size',		'10');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'context.format.wordWrap', 						'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.svt',					'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'start.situation.triggers.at.startup', 			'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'copyObjects.setStatusOff',						'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'ldap.login.prefix', 							'some-prefix');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'max.connection.timeout', 						'5000');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'scheduled.cleanup.delayMinutes', 				'15');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'monitoring.pagination.size', 					'20');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'scheduled.cleanup.daysRemaining', 				'14');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'atp.project.name', 							null);
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'callchain.parallel.running.thread.count', 		'10');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.link.KeyBasedLinkCollector.url.format',	'/solutions/jumpToIntegrationSession.jsp?processId=%s');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.runValidation', 		'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'test.server.availability',						'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'message.pretty.format', 						'true');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.execution.enabled',						'true');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.link.KeyBasedLinkCollector.key', 		'tc.Keys.processId');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'context.format.messageType', 					'XML');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'startParam.starterObject.makeDefaultDataset', 	'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'tc.timeout.fail', 								'20');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'folder.delete.notempty.allow',					'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.to.atp.enabled', 						'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'context.format.expandAll',						'true');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'start.transport.triggers.at.startup', 			'false');
insert into mb_project_settings(project_id, prop_short_name, prop_value) values(project_id, 'report.in.different.thread', 					'false');
--Set default project settings - End
END;
$$;


--
-- Name: get_all_env_triggers(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_all_env_triggers(env_id bigint) RETURNS TABLE(id bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
select mb_configuration.id from mb_configuration where parent_conf_id in (
    select mb_configuration.id from mb_configuration
    where (parent_in_server_id, transport_id) in
          ( select c.servers, c.transport_id
            from (select a.servers, a.systems, b.id transport_id from mb_env_inbound a
                                                                          inner join mb_configuration b on b.parent_system_id=a.systems
                  where a.environment_id = env_id) c) );
END;
$$;


--
-- Name: get_all_env_triggers_by_parentconf(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_all_env_triggers_by_parentconf(this_parent_conf_id bigint) RETURNS TABLE(id bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
select mb_configuration.id from mb_configuration where parent_conf_id in (
    select mb_configuration.id from mb_configuration where (parent_in_server_id, transport_id) in
                                                           ( select c.servers, c.transport_id from (
                                                                                                       select a.servers, a.systems, b.id transport_id from mb_env_inbound a
                                                                                                                                                               inner join mb_configuration b on b.parent_system_id=a.systems
                                                                                                       where a.environment_id in (
                                                                                                           select environment_id from mb_env_inbound where (servers,systems) in
                                                                                                                                                           (
                                                                                                                                                               select a.parent_in_server_id, b.parent_system_id from mb_configuration a
                                                                                                                                                                                                                         join mb_configuration b on b.id=a.transport_id
                                                                                                                                                               where a.id = this_parent_conf_id
                                                                                                                                                           )
                                                                                                       )
                                                                                                   ) c) );
END;
$$;


--
-- Name: get_all_triggers(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_all_triggers(trigger_id bigint) RETURNS TABLE(id bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
select mb_configuration.id from mb_configuration where parent_conf_id in (
    select mb_configuration.id from mb_configuration where (parent_in_server_id, transport_id) in
                                                           ( select c.servers, c.transport_id from (
                                                                                                       select a.servers, a.systems, b.id transport_id from mb_env_inbound a
                                                                                                                                                               inner join mb_configuration b on b.parent_system_id=a.systems
                                                                                                       where a.environment_id in (
                                                                                                           select environment_id from mb_env_inbound where (servers,systems) in
                                                                                                                                                           (
                                                                                                                                                               select a.parent_in_server_id, b.parent_system_id from mb_configuration a
                                                                                                                                                                                                                         join mb_configuration b on b.id=a.transport_id
                                                                                                                                                               where a.id in (
                                                                                                                                                                   select parent_conf_id from mb_configuration where mb_configuration.id = trigger_id)
                                                                                                                                                           )
                                                                                                       )
                                                                                                   ) c) );
END;
$$;


--
-- Name: get_env(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_env(trigger_id bigint) RETURNS TABLE(id bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
select env_inbound.environment_id
from mb_configuration inbound
         inner join mb_configuration trig ON (inbound.id = trig.parent_conf_id and trig.id = trigger_id )
         inner join	mb_configuration transport ON (inbound.transport_id = transport.id)
         inner join mb_env_inbound env_inbound ON (env_inbound.servers = inbound.parent_in_server_id and env_inbound.systems = transport.parent_system_id);
END
$$;


--
-- Name: get_env_by_parentconf(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_env_by_parentconf(this_parent_conf_id bigint) RETURNS TABLE(id bigint)
    LANGUAGE plpgsql
    AS $$

BEGIN
RETURN QUERY
select env_inbound.environment_id from mb_configuration inbound
                                           inner join	mb_configuration transport ON (inbound.transport_id = transport.id)
                                           inner join mb_env_inbound env_inbound ON (env_inbound.servers = inbound.parent_in_server_id and env_inbound.systems = transport.parent_system_id)
where inbound.id = this_parent_conf_id;

END
$$;


--
-- Name: get_switch_date(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_switch_date() RETURNS timestamp without time zone
    LANGUAGE sql IMMUTABLE
    AS $$
select cast(to_timestamp('01012009','MMDDYYYY') as timestamp)
           $$;


--
-- Name: gethostid(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.gethostid() RETURNS numeric
    LANGUAGE sql
    AS $_$select coalesce ((select value from mb$host_id limit 1), 11)$_$;


--
-- Name: getid(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.getid() RETURNS numeric
    LANGUAGE plpgsql
    AS $$
declare
SWITCH_DATE constant timestamp := get_switch_date();
    cdate timestamp := current_timestamp;
    v_sq   numeric;
begin
select /*+getid*/ nextval('sqsystem') into v_sq;

RETURN TO_CHAR(91232000000 + cast(round(extract(epoch from (cdate-SWITCH_DATE))) as numeric),'99999999999')
    || TRIM (TO_CHAR (gethostid(), '00'))
    || TRIM (TO_CHAR (v_sq, '000000'));
END;
$$;


--
-- Name: hex_to_int2(text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.hex_to_int2(text) RETURNS integer
    LANGUAGE sql
    AS $_$
select ('x'||substr(md5($1),1,8))::bit(6)::int;
$_$;


--
-- Name: increment_range(bigint, bigint, bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.increment_range(curval bigint, inc bigint, maxval bigint) RETURNS bigint
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN least(curval + inc, maxval);
EXCEPTION
    WHEN OTHERS THEN
        RETURN maxval;
END;
$$;


--
-- Name: update_env_state_after_inbound_delete(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_env_state_after_inbound_delete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
r mb_configuration%rowtype;
    env_state character varying:='EMPTY';
BEGIN
FOR r IN (select * from mb_configuration where id in (select * from get_all_env_triggers(old.environment_id) ))
        LOOP
            env_state := add_state(env_state, r.trigger_state);
END LOOP;
update mb_env set state = env_state where id = old.environment_id;

Return NEW;
END;
$$;


--
-- Name: update_env_state_after_inbound_insert_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_env_state_after_inbound_insert_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
r mb_configuration%rowtype;
    env_state character varying:='EMPTY';
BEGIN
FOR r IN (select * from mb_configuration where id in (select * from get_all_env_triggers(new.environment_id) ))
        LOOP
            env_state := add_state(env_state, r.trigger_state);
END LOOP;
update mb_env set state = env_state where id = new.environment_id;

Return NEW;
END;
$$;


--
-- Name: update_env_state_after_trigger_delete(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_env_state_after_trigger_delete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
r mb_configuration%rowtype;
    env_state character varying:='EMPTY';
BEGIN
FOR r IN (select * from mb_configuration where id != old.id and id in (select * from get_all_env_triggers_by_parentconf(old.parent_conf_id) ))
        LOOP
            env_state := add_state(env_state, r.trigger_state);
END LOOP;
update mb_env set state = env_state where id in (select * from get_env_by_parentconf(old.parent_conf_id));

Return OLD;
END;
$$;


--
-- Name: update_env_state_after_trigger_insert(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_env_state_after_trigger_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
r mb_configuration%rowtype;
    env_state character varying:='EMPTY';
BEGIN
    env_state := add_state(env_state, new.trigger_state);
FOR r IN (select * from mb_configuration where id != new.id and id in (select * from get_all_triggers(new.id) ))
        LOOP
            env_state := add_state(env_state, r.trigger_state);
END LOOP;
update mb_env set state = env_state where id in  (select * from get_env(NEW.id));

Return NEW;
END;
$$;


--
-- Name: update_env_state_after_trigger_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_env_state_after_trigger_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
r mb_configuration%rowtype;
    env_state character varying:='EMPTY';
BEGIN
    env_state := add_state(env_state, new.trigger_state);
FOR r IN (select * from mb_configuration where id != OLD.id and id in (select * from get_all_triggers(OLD.id) ))
        LOOP
            env_state := add_state(env_state, r.trigger_state);
END LOOP;
update mb_env set state = env_state where id in  (select * from get_env(OLD.id));
Return NEW;
END;
$$;


SET default_tablespace = '';

-- Commented by KAG 2023-12-07 to ensure db creation on PostgreSQL 10.x.
-- 	- In fact, this command means nothing, because default parameter value is 'heap' - so, senseless to set it explicitly.
-- SET default_table_access_method = heap;


--
-- Name: entities_migration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.entities_migration (
                                           id uuid NOT NULL,
                                           date timestamp without time zone,
                                           status character varying(255),
                                           description character varying(255)
);


--
-- Name: history_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.history_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mb$host_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."mb$host_id" (
    value numeric(2,0) NOT NULL
);


--
-- Name: mb_bv_cases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_bv_cases (
                                    callchain_id bigint NOT NULL,
                                    ds_name character varying(255) NOT NULL,
                                    bv_tcid character varying(255)
);


--
-- Name: mb_chain; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_chain (
                                 id bigint NOT NULL,
                                 dataset_id character varying(255),
                                 parent_id bigint,
                                 project_id bigint
);


--
-- Name: mb_chain_keys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_chain_keys (
                                      parent_id bigint NOT NULL,
                                      key character varying(255)
);


--
-- Name: mb_chain_labels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_chain_labels (
                                        id bigint,
                                        labels character varying(255)
);


--
-- Name: mb_compatible_ds_lists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_compatible_ds_lists (
                                               callchain_id bigint NOT NULL,
                                               datasetlist_id character varying(255)
);


--
-- Name: mb_compatible_with_transports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_compatible_with_transports (
                                                      parent_id bigint NOT NULL,
                                                      key character varying(255)
);


--
-- Name: mb_condition_params; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_condition_params (
                                            id bigint NOT NULL,
                                            version integer NOT NULL,
                                            parent_type character varying(255) NOT NULL,
                                            name character varying(255),
                                            description character varying(255),
                                            natural_id character varying(255),
                                            order_id integer,
                                            value character varying(255),
                                            condition character varying(255),
                                            etc character varying(255),
                                            parent_id bigint
);


--
-- Name: mb_condition_props; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_condition_props (
                                           id bigint NOT NULL,
                                           version integer NOT NULL,
                                           condition_type character varying(255) NOT NULL,
                                           name character varying(255),
                                           description character varying(255),
                                           natural_id character varying(255),
                                           trigger_id bigint,
                                           step_id bigint,
                                           condition_parameters text
);


--
-- Name: mb_configuration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_configuration (
                                         id bigint NOT NULL,
                                         version integer NOT NULL,
                                         type character varying(255) NOT NULL,
                                         name character varying(255),
                                         description character varying(255),
                                         natural_id character varying(255),
                                         type_name character varying(255),
                                         parent_system_id bigint,
                                         ec_id character varying(255),
                                         ec_project_id character varying(255),
                                         mep character varying(255),
                                         parent_in_server_id bigint,
                                         transport_id bigint,
                                         parent_out_server_id bigint,
                                         system_id bigint,
                                         parent_conf_id bigint,
                                         trigger_state character varying(255),
                                         activation_error_message character varying,
                                         parent_project_id bigint,
                                         parent_env_id bigint,
                                         parent_template_id bigint,
                                         interceptor_id bigint,
                                         interceptor_transport character varying(255),
                                         applicability_params_interceptor_id bigint,
                                         ei_session_parent_id bigint,
                                         params character varying
);


--
-- Name: mb_configurations_param; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_configurations_param (
                                                configuration_id bigint NOT NULL,
                                                prop_short_name character varying(255) NOT NULL,
                                                prop_value text
);


--
-- Name: mb_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_context (
                                   id bigint NOT NULL,
                                   type character varying(255) NOT NULL,
                                   name character varying(255),
                                   description character varying(255),
                                   natural_id character varying(255),
                                   prefix character varying(255),
                                   extensions text,
                                   json_string text,
                                   start_time timestamp without time zone,
                                   initiator_id bigint,
                                   project_id bigint,
                                   environment_id bigint,
                                   environment_name character varying(255),
                                   status character varying(255),
                                   end_time timestamp without time zone,
                                   client character varying(255),
                                   step_id bigint,
                                   incoming_message_id bigint,
                                   outgoing_message_id bigint,
                                   parent_ctx_id bigint,
                                   validation_results text,
                                   session_id character varying(255),
                                   instance bigint,
                                   tc_id bigint,
                                   last_update_time bigint,
                                   time_to_live bigint,
                                   pod_name character varying(255)
);


--
-- Name: mb_context_binding_keys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_context_binding_keys (
                                                id bigint NOT NULL,
                                                key character varying(255)
);


--
-- Name: mb_context_report_links; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_context_report_links (
                                                parent_id bigint NOT NULL,
                                                key text NOT NULL,
                                                value text
);


--
-- Name: mb_counter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_counter (
                                   id bigint NOT NULL,
                                   type character varying(255) NOT NULL,
                                   name character varying(255),
                                   natural_id character varying(255),
                                   data date,
                                   index integer,
                                   format character varying(255)
);


--
-- Name: mb_counter_owners; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_counter_owners (
                                          counter_id bigint NOT NULL,
                                          owner bigint
);


--
-- Name: mb_end_situations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_end_situations (
                                          step_id bigint NOT NULL,
                                          situation_id bigint NOT NULL
);


--
-- Name: mb_env; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_env (
                               id bigint NOT NULL,
                               version integer NOT NULL,
                               name character varying(255),
                               description character varying(255),
                               natural_id character varying(255),
                               ec_id character varying(255),
                               ec_project_id character varying(255),
                               parent_id bigint,
                               state character varying(255),
                               project_id bigint
);


--
-- Name: mb_env_inbound; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_env_inbound (
                                       environment_id bigint NOT NULL,
                                       systems bigint NOT NULL,
                                       servers bigint NOT NULL
);


--
-- Name: mb_env_outbound; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_env_outbound (
                                        environment_id bigint NOT NULL,
                                        systems bigint NOT NULL,
                                        servers bigint NOT NULL
);


--
-- Name: mb_exceptional_situation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_exceptional_situation (
                                                 step_id bigint NOT NULL,
                                                 situation_id bigint NOT NULL
);


--
-- Name: mb_folders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_folders (
                                   id bigint NOT NULL,
                                   version integer NOT NULL,
                                   type character varying(255) NOT NULL,
                                   name character varying(255),
                                   description character varying(255),
                                   natural_id character varying(255),
                                   type_name character varying(255),
                                   project_id bigint,
                                   parent_id bigint
);


--
-- Name: mb_folders_labels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_folders_labels (
                                          id bigint,
                                          labels character varying(255)
);


--
-- Name: mb_headers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_headers (
                                   parent_id bigint NOT NULL,
                                   prop_short_name character varying(255) NOT NULL,
                                   prop_value text
);


--
-- Name: mb_history_object_params; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_history_object_params (
                                                 id bigint NOT NULL,
                                                 name character varying(255),
                                                 natural_id character varying(255),
                                                 change_id character varying(255),
                                                 object_id character varying(255),
                                                 parameter_name character varying(255),
                                                 old_value text,
                                                 new_value text,
                                                 modified_when timestamp without time zone,
                                                 modified_by character varying(255)
);


--
-- Name: mb_history_objects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_history_objects (
                                           id bigint NOT NULL,
                                           name character varying(255),
                                           description character varying(255),
                                           natural_id character varying(255),
                                           object_id character varying(255),
                                           object_type character varying(255),
                                           history_parent_id character varying(255),
                                           changed_when timestamp without time zone,
                                           changed_by character varying(255)
);


--
-- Name: mb_install_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_install_history (
                                           release_version character varying(30) NOT NULL,
                                           script_type character varying(12) NOT NULL,
                                           execution_date timestamp with time zone NOT NULL,
                                           filename text NOT NULL
);


--
-- Name: mb_instance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_instance (
                                    id bigint NOT NULL,
                                    type character varying(255) NOT NULL,
                                    name character varying(255),
                                    natural_id character varying(255),
                                    status character varying(255),
                                    start_time timestamp without time zone,
                                    end_time timestamp without time zone,
                                    error_name text,
                                    error_message text,
                                    extensions text,
                                    parent_id bigint,
                                    step_id bigint,
                                    context_id bigint,
                                    situation_id bigint,
                                    operation_name character varying(255),
                                    system_name character varying(255),
                                    chain_id bigint,
                                    dataset_name text,
                                    callchain_execution_data text,
                                    system_id bigint,
                                    part_num integer
);


--
-- Name: mb_interceptors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_interceptors (
                                        id bigint NOT NULL,
                                        version integer NOT NULL,
                                        type character varying(255) NOT NULL,
                                        name character varying(255),
                                        type_name character varying(255),
                                        natural_id character varying(255),
                                        description character varying(255),
                                        transport_name character varying(255),
                                        active boolean,
                                        order_number integer,
                                        interceptor_group character varying(255),
                                        parent_transport_id bigint,
                                        parent_template_id bigint
);


--
-- Name: mb_message; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message (
                                   id bigint NOT NULL,
                                   natural_id character varying(255),
                                   text text
);


--
-- Name: mb_message_connection_properties; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_connection_properties (
                                                         parent_id bigint NOT NULL,
                                                         key character varying(255) NOT NULL,
                                                         value character varying(255)
);


--
-- Name: mb_message_headers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_headers (
                                           parent_id bigint NOT NULL,
                                           key character varying(255) NOT NULL,
                                           value text
);


--
-- Name: mb_message_param; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_param (
                                         id bigint NOT NULL,
                                         natural_id character varying(255),
                                         param_name character varying(255),
                                         multiple boolean,
                                         context_id bigint
);


--
-- Name: mb_message_param_multiple_value; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_param_multiple_value (
                                                        message_param_id bigint,
                                                        value text
);


--
-- Name: mb_operations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_operations (
                                      id bigint NOT NULL,
                                      definition_key character varying(255),
                                      transport_id bigint,
                                      default_inbound_situation bigint,
                                      error_inbound_situation bigint,
                                      parent_id bigint,
                                      incoming_text_context_key_definition text,
                                      outgoing_text_context_key_definition text,
                                      name character varying(255),
                                      description character varying(255),
                                      natural_id character varying(255),
                                      project_id bigint,
                                      version integer NOT NULL
);


--
-- Name: mb_parsing_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_parsing_rules (
                                         id bigint NOT NULL,
                                         version integer NOT NULL,
                                         natural_id character varying(255),
                                         name character varying(255),
                                         description character varying(255),
                                         multiple boolean,
                                         param_name character varying(255),
                                         expression text,
                                         autosave boolean,
                                         rule_type character varying(255),
                                         project_id bigint,
                                         parent_system_id bigint,
                                         parent_operation_id bigint,
                                         type character varying(20) NOT NULL
);


--
-- Name: mb_project_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_project_settings (
                                            project_id bigint NOT NULL,
                                            prop_short_name character varying(255) NOT NULL,
                                            prop_value text
);


--
-- Name: mb_projects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_projects (
                                    id bigint NOT NULL,
                                    version integer NOT NULL,
                                    name character varying(255),
                                    description character varying(255),
                                    natural_id character varying(255),
                                    uuid uuid,
                                    systems_folder bigint,
                                    chains_folder bigint,
                                    envs_folder bigint,
                                    servers_folder bigint,
                                    users_folder bigint
);


--
-- Name: mb_servers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_servers (
                                   id bigint NOT NULL,
                                   version integer NOT NULL,
                                   name character varying(255),
                                   description character varying(255),
                                   natural_id character varying(255),
                                   ec_id character varying(255),
                                   ec_project_id character varying(255),
                                   parent_id bigint,
                                   url character varying(255),
                                   project_id bigint
);


--
-- Name: mb_situation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_situation (
                                     id bigint NOT NULL,
                                     parent_id bigint,
                                     validate_incoming character varying(10),
                                     bv_tcid character varying(255),
                                     pre_script text,
                                     post_script text,
                                     pre_validation_script text,
                                     ignore_errors boolean
);


--
-- Name: mb_situation_keys_to_regenerate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_situation_keys_to_regenerate (
                                                        parent_id bigint NOT NULL,
                                                        key character varying(255) NOT NULL,
                                                        script text
);


--
-- Name: mb_situation_labels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_situation_labels (
                                            id bigint,
                                            labels character varying(255)
);


--
-- Name: mb_situations_parsingrules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_situations_parsingrules (
                                                   situation_id bigint NOT NULL,
                                                   parsingrule_id bigint NOT NULL
);


--
-- Name: mb_step_container; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_step_container (
                                          id bigint NOT NULL,
                                          version integer NOT NULL,
                                          name character varying(255),
                                          description character varying(255),
                                          natural_id character varying(255)
);


--
-- Name: mb_steps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_steps (
                                 id bigint NOT NULL,
                                 version integer NOT NULL,
                                 type character varying(255) NOT NULL,
                                 name character varying(255),
                                 description character varying(255),
                                 natural_id character varying(255),
                                 delay bigint,
                                 unit character varying(20),
                                 enabled boolean,
                                 manual boolean,
                                 order_number integer,
                                 parent_id bigint,
                                 situation_id bigint,
                                 wait_all_end_situations boolean,
                                 retry_on_fail boolean,
                                 retry_timeout_unit character varying(20),
                                 retry_timeout bigint,
                                 validation_max_attempts integer,
                                 validation_max_time bigint,
                                 validation_unit_max_time character varying(20),
                                 condition_max_attempts integer,
                                 condition_max_time bigint,
                                 condition_unit_max_time character varying(20),
                                 condition_retry boolean,
                                 pre_script text,
                                 chain_id bigint,
                                 sender_id bigint,
                                 receiver_id bigint,
                                 operation_id bigint,
                                 sys_template_id bigint,
                                 op_template_id bigint
);


--
-- Name: mb_steps_keys_to_regenerate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_steps_keys_to_regenerate (
                                                    parent_id bigint NOT NULL,
                                                    key character varying(255) NOT NULL,
                                                    script text
);


--
-- Name: mb_systems; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_systems (
                                   id bigint NOT NULL,
                                   parent_id bigint,
                                   ec_id character varying(255),
                                   ec_label character varying(255),
                                   ec_project_id character varying(255),
                                   incoming_text_context_key_definition text,
                                   outgoing_text_context_key_definition text,
                                   operation_text_key_definition text,
                                   name character varying(255),
                                   description character varying(255),
                                   natural_id character varying(255),
                                   project_id bigint,
                                   version integer NOT NULL
);


--
-- Name: mb_systems_labels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_systems_labels (
                                          id bigint,
                                          labels character varying(255)
);


--
-- Name: mb_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_templates (
                                     id bigint NOT NULL,
                                     version integer NOT NULL,
                                     name character varying(255),
                                     description character varying(255),
                                     natural_id character varying(255),
                                     text text,
                                     project_id bigint,
                                     parent_system_id bigint,
                                     parent_operation_id bigint,
                                     type character varying(20) NOT NULL
);


--
-- Name: mb_templates_labels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_templates_labels (
                                            id bigint,
                                            labels character varying(255)
);


--
-- Name: mb_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_triggers (
                                    id bigint NOT NULL,
                                    version integer NOT NULL,
                                    parent_type character varying(255) NOT NULL,
                                    name character varying(255),
                                    description character varying(255),
                                    natural_id character varying(255),
                                    state character varying(255),
                                    priority integer,
                                    trigger_on character varying(255),
                                    situation_id bigint,
                                    oet_parent_id bigint,
                                    set_parent_id bigint
);


--
-- Name: mb_upgrade_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_upgrade_history (
                                           id bigint NOT NULL,
                                           upgrade_datetime timestamp without time zone,
                                           build_number character varying(255),
                                           natural_id character varying(255)
);


--
-- Name: mb_user_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_user_settings (
                                         id bigint NOT NULL,
                                         version integer NOT NULL,
                                         setting_id uuid,
                                         user_id character varying(255),
                                         setting_type character varying(255),
                                         property_name character varying(255),
                                         property_value character varying(255),
                                         name character varying(255),
                                         description character varying(255),
                                         natural_id character varying(255)
);


--
-- Name: mb_userdata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_userdata (
                                    project_id bigint NOT NULL,
                                    userkey character varying(50) NOT NULL,
                                    value character varying
);


--
-- Name: shedlock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shedlock (
                                 name character varying(64) NOT NULL,
                                 lock_until timestamp without time zone NOT NULL,
                                 locked_at timestamp without time zone NOT NULL,
                                 locked_by character varying(255) NOT NULL
);


--
-- Name: sqsystem; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sqsystem
    START WITH 870000
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 999999
    CACHE 100
    CYCLE;

--
-- Name: entities_migration entities_migration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entities_migration
    ADD CONSTRAINT entities_migration_pkey PRIMARY KEY (id);


--
-- Name: mb_bv_cases mb_bv_cases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_bv_cases
    ADD CONSTRAINT mb_bv_cases_pkey PRIMARY KEY (callchain_id, ds_name);


--
-- Name: mb_chain mb_chain_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_chain
    ADD CONSTRAINT mb_chain_pkey PRIMARY KEY (id);


--
-- Name: mb_condition_params mb_condition_params_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_condition_params
    ADD CONSTRAINT mb_condition_params_pkey PRIMARY KEY (id);


--
-- Name: mb_condition_props mb_condition_props_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_condition_props
    ADD CONSTRAINT mb_condition_props_pkey PRIMARY KEY (id);


--
-- Name: mb_configuration mb_configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT mb_configuration_pkey PRIMARY KEY (id);


--
-- Name: mb_configurations_param mb_configurations_param_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configurations_param
    ADD CONSTRAINT mb_configurations_param_pkey PRIMARY KEY (configuration_id, prop_short_name);


--
-- Name: mb_context mb_context_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_pkey PRIMARY KEY (id);


--
-- Name: mb_context_report_links mb_context_report_links_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_report_links
    ADD CONSTRAINT mb_context_report_links_pkey PRIMARY KEY (parent_id, key);


--
-- Name: mb_counter mb_counter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_counter
    ADD CONSTRAINT mb_counter_pkey PRIMARY KEY (id);


--
-- Name: mb_end_situations mb_end_situations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_end_situations
    ADD CONSTRAINT mb_end_situations_pkey PRIMARY KEY (step_id, situation_id);


--
-- Name: mb_env_inbound mb_env_inbound_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_inbound
    ADD CONSTRAINT mb_env_inbound_pkey PRIMARY KEY (environment_id, systems);


--
-- Name: mb_env_outbound mb_env_outbound_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_outbound
    ADD CONSTRAINT mb_env_outbound_pkey PRIMARY KEY (environment_id, systems);


--
-- Name: mb_env mb_env_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env
    ADD CONSTRAINT mb_env_pkey PRIMARY KEY (id);


--
-- Name: mb_exceptional_situation mb_exceptional_situation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_exceptional_situation
    ADD CONSTRAINT mb_exceptional_situation_pkey PRIMARY KEY (step_id, situation_id);


--
-- Name: mb_folders mb_folders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_folders
    ADD CONSTRAINT mb_folders_pkey PRIMARY KEY (id);


--
-- Name: mb_headers mb_headers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_headers
    ADD CONSTRAINT mb_headers_pkey PRIMARY KEY (parent_id, prop_short_name);


--
-- Name: mb_history_object_params mb_history_object_params_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_history_object_params
    ADD CONSTRAINT mb_history_object_params_pkey PRIMARY KEY (id);


--
-- Name: mb_history_objects mb_history_objects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_history_objects
    ADD CONSTRAINT mb_history_objects_pkey PRIMARY KEY (id);


--
-- Name: mb_install_history mb_install_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_install_history
    ADD CONSTRAINT mb_install_history_pk PRIMARY KEY (release_version, script_type, filename);


--
-- Name: mb_instance mb_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_instance
    ADD CONSTRAINT mb_instance_pkey PRIMARY KEY (id);


--
-- Name: mb_interceptors mb_interceptors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_interceptors
    ADD CONSTRAINT mb_interceptors_pkey PRIMARY KEY (id);


--
-- Name: mb_message_connection_properties mb_message_connection_properties_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_connection_properties
    ADD CONSTRAINT mb_message_connection_properties_pkey PRIMARY KEY (parent_id, key);


--
-- Name: mb_message_headers mb_message_headers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_headers
    ADD CONSTRAINT mb_message_headers_pkey PRIMARY KEY (parent_id, key);


--
-- Name: mb_message_param mb_message_param_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_param
    ADD CONSTRAINT mb_message_param_pkey PRIMARY KEY (id);


--
-- Name: mb_message mb_message_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message
    ADD CONSTRAINT mb_message_pkey PRIMARY KEY (id);


--
-- Name: mb_operations mb_operations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_operations
    ADD CONSTRAINT mb_operations_pkey PRIMARY KEY (id);


--
-- Name: mb_parsing_rules mb_parsing_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_parsing_rules
    ADD CONSTRAINT mb_parsing_rules_pkey PRIMARY KEY (id);


--
-- Name: mb_project_settings mb_project_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_project_settings
    ADD CONSTRAINT mb_project_settings_pkey PRIMARY KEY (project_id, prop_short_name);


--
-- Name: mb_projects mb_projects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_projects
    ADD CONSTRAINT mb_projects_pkey PRIMARY KEY (id);


--
-- Name: mb_servers mb_servers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_servers
    ADD CONSTRAINT mb_servers_pkey PRIMARY KEY (id);


--
-- Name: mb_situation_keys_to_regenerate mb_situation_keys_to_regenerate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situation_keys_to_regenerate
    ADD CONSTRAINT mb_situation_keys_to_regenerate_pkey PRIMARY KEY (parent_id, key);


--
-- Name: mb_situation mb_situation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situation
    ADD CONSTRAINT mb_situation_pkey PRIMARY KEY (id);


--
-- Name: mb_situations_parsingrules mb_situations_parsingrules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situations_parsingrules
    ADD CONSTRAINT mb_situations_parsingrules_pkey PRIMARY KEY (situation_id, parsingrule_id);


--
-- Name: mb_step_container mb_step_container_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_step_container
    ADD CONSTRAINT mb_step_container_pkey PRIMARY KEY (id);


--
-- Name: mb_steps_keys_to_regenerate mb_steps_keys_to_regenerate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps_keys_to_regenerate
    ADD CONSTRAINT mb_steps_keys_to_regenerate_pkey PRIMARY KEY (parent_id, key);


--
-- Name: mb_steps mb_steps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT mb_steps_pkey PRIMARY KEY (id);


--
-- Name: mb_systems mb_systems_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_systems
    ADD CONSTRAINT mb_systems_pkey PRIMARY KEY (id);


--
-- Name: mb_templates mb_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_templates
    ADD CONSTRAINT mb_templates_pkey PRIMARY KEY (id);


--
-- Name: mb_triggers mb_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_triggers
    ADD CONSTRAINT mb_triggers_pkey PRIMARY KEY (id);


--
-- Name: mb_upgrade_history mb_upgrade_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_upgrade_history
    ADD CONSTRAINT mb_upgrade_history_pkey PRIMARY KEY (id);


--
-- Name: mb_user_settings mb_user_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_user_settings
    ADD CONSTRAINT mb_user_settings_pkey PRIMARY KEY (id);


--
-- Name: mb_userdata mb_userdata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_userdata
    ADD CONSTRAINT mb_userdata_pkey PRIMARY KEY (project_id, userkey);


--
-- Name: shedlock shedlock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shedlock
    ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);


--
-- Name: callchain_by_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX callchain_by_project ON public.mb_chain USING btree (project_id);


--
-- Name: callchain_by_project_and_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX callchain_by_project_and_parent ON public.mb_chain USING btree (project_id, parent_id);


--
-- Name: configurations_by_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX configurations_by_type ON public.mb_configuration USING btree (type);


--
-- Name: env_ec_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX env_ec_id ON public.mb_env USING btree (ec_id);


--
-- Name: env_ec_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX env_ec_project_id ON public.mb_env USING btree (ec_project_id);


--
-- Name: envs_by_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX envs_by_project ON public.mb_env USING btree (project_id);


--
-- Name: envs_by_project_and_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX envs_by_project_and_parent ON public.mb_env USING btree (project_id, parent_id);


--
-- Name: folders_by_project_and_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX folders_by_project_and_type ON public.mb_folders USING btree (project_id, type);


--
-- Name: ind_end_situations_situation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ind_end_situations_situation ON public.mb_end_situations USING btree (situation_id, step_id);


--
-- Name: ind_exceptional_situations_situation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ind_exceptional_situations_situation ON public.mb_exceptional_situation USING btree (situation_id, step_id);


--
-- Name: ind_steps_situation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ind_steps_situation ON public.mb_steps USING btree (situation_id, id);


--
-- Name: mb_chain_by_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_chain_by_parent ON public.mb_chain USING btree (parent_id);


--
-- Name: mb_chain_keys_by_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_chain_keys_by_parent ON public.mb_chain_keys USING btree (parent_id);


--
-- Name: mb_chain_labels_by_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_chain_labels_by_id ON public.mb_chain_labels USING btree (id);


--
-- Name: mb_compatible_ds_lists_by_callchain_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_compatible_ds_lists_by_callchain_id ON public.mb_compatible_ds_lists USING btree (callchain_id);


--
-- Name: mb_compatible_with_transports_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_compatible_with_transports_by_parent_id ON public.mb_compatible_with_transports USING btree (parent_id);


--
-- Name: mb_condition_params_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_condition_params_by_parent_id ON public.mb_condition_params USING btree (parent_id);


--
-- Name: mb_condition_props_by_step_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_condition_props_by_step_id ON public.mb_condition_props USING btree (step_id);


--
-- Name: mb_condition_props_by_trigger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_condition_props_by_trigger_id ON public.mb_condition_props USING btree (trigger_id);


--
-- Name: mb_configuration_by_applicability_params_interceptor_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_applicability_params_interceptor_id ON public.mb_configuration USING btree (applicability_params_interceptor_id);


--
-- Name: mb_configuration_by_ei_session_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_ei_session_parent_id ON public.mb_configuration USING btree (ei_session_parent_id);


--
-- Name: mb_configuration_by_interceptor_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_interceptor_id ON public.mb_configuration USING btree (interceptor_id);


--
-- Name: mb_configuration_by_parent_conf_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_conf_id ON public.mb_configuration USING btree (parent_conf_id);


--
-- Name: mb_configuration_by_parent_env_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_env_id ON public.mb_configuration USING btree (parent_env_id);


--
-- Name: mb_configuration_by_parent_in_server_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_in_server_id ON public.mb_configuration USING btree (parent_in_server_id);


--
-- Name: mb_configuration_by_parent_out_server_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_out_server_id ON public.mb_configuration USING btree (parent_out_server_id);


--
-- Name: mb_configuration_by_parent_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_project_id ON public.mb_configuration USING btree (parent_project_id);


--
-- Name: mb_configuration_by_parent_system_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_system_id ON public.mb_configuration USING btree (parent_system_id);


--
-- Name: mb_configuration_by_parent_template_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_parent_template_id ON public.mb_configuration USING btree (parent_template_id);


--
-- Name: mb_configuration_by_system_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_system_id ON public.mb_configuration USING btree (system_id);


--
-- Name: mb_configuration_by_transport_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_configuration_by_transport_id ON public.mb_configuration USING btree (transport_id);


--
-- Name: mb_configuration_integrations_by_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX mb_configuration_integrations_by_id ON public.mb_configuration USING btree (id) WHERE ((type)::text = 'integration'::text);


--
-- Name: mb_counter_owners_counter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_counter_owners_counter ON public.mb_counter_owners USING btree (counter_id);


--
-- Name: mb_counter_owners_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_counter_owners_owner ON public.mb_counter_owners USING btree (owner);


--
-- Name: mb_env_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_env_by_parent_id ON public.mb_env USING btree (parent_id);


--
-- Name: mb_env_inbound_by_servers; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_env_inbound_by_servers ON public.mb_env_inbound USING btree (servers);


--
-- Name: mb_env_inbound_by_systems; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_env_inbound_by_systems ON public.mb_env_inbound USING btree (systems);


--
-- Name: mb_env_outbound_by_servers; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_env_outbound_by_servers ON public.mb_env_outbound USING btree (servers);


--
-- Name: mb_env_outbound_by_systems; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_env_outbound_by_systems ON public.mb_env_outbound USING btree (systems);


--
-- Name: mb_folders_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_folders_by_parent_id ON public.mb_folders USING btree (parent_id);


--
-- Name: mb_folders_by_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_folders_by_project_id ON public.mb_folders USING btree (project_id);


--
-- Name: mb_folders_labels_by_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_folders_labels_by_id ON public.mb_folders_labels USING btree (id);


--
-- Name: mb_folders_labels_by_labels; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_folders_labels_by_labels ON public.mb_folders_labels USING btree (labels);


--
-- Name: mb_history_object_params_by_object_modified; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_history_object_params_by_object_modified ON public.mb_history_object_params USING btree (object_id, modified_when);


--
-- Name: mb_history_objects_by_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_history_objects_by_name ON public.mb_history_objects USING btree (name);


--
-- Name: mb_history_objects_by_object; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_history_objects_by_object ON public.mb_history_objects USING btree (object_id);


--
-- Name: mb_history_objects_by_parent_changed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_history_objects_by_parent_changed ON public.mb_history_objects USING btree (history_parent_id, changed_when);


--
-- Name: mb_interceptors_by_parent_template_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_interceptors_by_parent_template_id ON public.mb_interceptors USING btree (parent_template_id);


--
-- Name: mb_interceptors_by_parent_transport_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_interceptors_by_parent_transport_id ON public.mb_interceptors USING btree (parent_transport_id);


--
-- Name: mb_labels_name_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_labels_name_idx ON public.mb_chain_labels USING btree (labels);


--
-- Name: mb_operations_by_default_inbound_situation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_operations_by_default_inbound_situation ON public.mb_operations USING btree (default_inbound_situation);


--
-- Name: mb_operations_by_error_inbound_situation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_operations_by_error_inbound_situation ON public.mb_operations USING btree (error_inbound_situation);


--
-- Name: mb_operations_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_operations_by_parent_id ON public.mb_operations USING btree (parent_id);


--
-- Name: mb_operations_by_transport_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_operations_by_transport_id ON public.mb_operations USING btree (transport_id);


--
-- Name: mb_servers_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_servers_by_parent_id ON public.mb_servers USING btree (parent_id);


--
-- Name: mb_situation_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_situation_by_parent_id ON public.mb_situation USING btree (parent_id);


--
-- Name: mb_steps_by_chain_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_steps_by_chain_id ON public.mb_steps USING btree (chain_id);


--
-- Name: mb_steps_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_steps_by_parent_id ON public.mb_steps USING btree (parent_id);


--
-- Name: mb_systems_by_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_systems_by_parent_id ON public.mb_systems USING btree (parent_id);


--
-- Name: mb_systems_labels_by_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_systems_labels_by_id ON public.mb_systems_labels USING btree (id);


--
-- Name: mb_systems_labels_by_labels; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_systems_labels_by_labels ON public.mb_systems_labels USING btree (labels);


--
-- Name: mb_templates_labels_by_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_templates_labels_by_id ON public.mb_templates_labels USING btree (id);


--
-- Name: mb_templates_labels_by_labels; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_templates_labels_by_labels ON public.mb_templates_labels USING btree (labels);


--
-- Name: mb_triggers_by_situation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_triggers_by_situation_id ON public.mb_triggers USING btree (situation_id);


--
-- Name: operations_by_parent_and_definitionkey; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX operations_by_parent_and_definitionkey ON public.mb_operations USING btree (parent_id, definition_key);


--
-- Name: operations_by_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX operations_by_project ON public.mb_operations USING btree (project_id);


--
-- Name: operationtriggers_by_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX operationtriggers_by_parent ON public.mb_triggers USING btree (oet_parent_id);


--
-- Name: parsingrules_by_parent_operation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX parsingrules_by_parent_operation ON public.mb_parsing_rules USING btree (parent_operation_id);


--
-- Name: parsingrules_by_parent_system; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX parsingrules_by_parent_system ON public.mb_parsing_rules USING btree (parent_system_id);


--
-- Name: parsingrules_by_project_and_parent_operation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX parsingrules_by_project_and_parent_operation ON public.mb_parsing_rules USING btree (project_id, parent_operation_id);


--
-- Name: parsingrules_by_project_and_parent_system; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX parsingrules_by_project_and_parent_system ON public.mb_parsing_rules USING btree (project_id, parent_system_id);


--
-- Name: projects_by_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX projects_by_uuid ON public.mb_projects USING btree (uuid);


--
-- Name: servers_by_project_and_url; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX servers_by_project_and_url ON public.mb_servers USING btree (project_id, url);


--
-- Name: servers_by_project_and_url_slashed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX servers_by_project_and_url_slashed ON public.mb_servers USING btree (project_id, (
    CASE
    WHEN ("right"((url)::text, 1) = '/'::text) THEN (url)::text
    ELSE ((url)::text || '/'::text)
    END));


--
-- Name: situation_parsing_rules_by_parsingrule; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX situation_parsing_rules_by_parsingrule ON public.mb_situations_parsingrules USING btree (parsingrule_id);


--
-- Name: situation_parsing_rules_by_situation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX situation_parsing_rules_by_situation ON public.mb_situations_parsingrules USING btree (situation_id);


--
-- Name: situations_by_bvtcid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX situations_by_bvtcid ON public.mb_situation USING btree (bv_tcid);


--
-- Name: situationtriggers_by_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX situationtriggers_by_parent ON public.mb_triggers USING btree (set_parent_id);


--
-- Name: steps_by_op_template; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX steps_by_op_template ON public.mb_steps USING btree (op_template_id);


--
-- Name: steps_by_operation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX steps_by_operation ON public.mb_steps USING btree (operation_id);


--
-- Name: steps_by_receiver; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX steps_by_receiver ON public.mb_steps USING btree (receiver_id);


--
-- Name: steps_by_sender; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX steps_by_sender ON public.mb_steps USING btree (sender_id);


--
-- Name: steps_by_sys_template; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX steps_by_sys_template ON public.mb_steps USING btree (sys_template_id);


--
-- Name: sys_ec_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX sys_ec_id ON public.mb_systems USING btree (ec_id);


--
-- Name: sys_ec_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX sys_ec_project_id ON public.mb_systems USING btree (ec_project_id);


--
-- Name: systems_by_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX systems_by_project ON public.mb_systems USING btree (project_id);


--
-- Name: systems_by_project_and_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX systems_by_project_and_parent ON public.mb_systems USING btree (project_id, parent_id);


--
-- Name: templates_by_parent_operation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX templates_by_parent_operation ON public.mb_templates USING btree (parent_operation_id);


--
-- Name: templates_by_parent_system; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX templates_by_parent_system ON public.mb_templates USING btree (parent_system_id);


--
-- Name: templates_by_project_and_lowername; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX templates_by_project_and_lowername ON public.mb_templates USING btree (project_id, lower((name)::text));


--
-- Name: templates_by_project_and_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX templates_by_project_and_name ON public.mb_templates USING btree (project_id, name);


--
-- Name: templates_by_project_and_parent_operation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX templates_by_project_and_parent_operation ON public.mb_templates USING btree (project_id, parent_operation_id);


--
-- Name: templates_by_project_and_parent_system; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX templates_by_project_and_parent_system ON public.mb_templates USING btree (project_id, parent_system_id);


--
-- Name: transport_ec_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX transport_ec_id ON public.mb_configuration USING btree (ec_id) WHERE ((type)::text = 'transport'::text);


--
-- Name: transport_ec_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX transport_ec_project_id ON public.mb_configuration USING btree (ec_project_id) WHERE ((type)::text = 'transport'::text);


--
-- Name: mb_env compute_env_state_before_env_update; Type: TRIGGER; Schema: public; Owner: -
--
--	Changed by KAG 2023-12-07 from 'EXECUTE FUNCTION' to 'EXECUTE PROCEDURE' to ensure creation on PostgreSQL 10.x

CREATE TRIGGER compute_env_state_before_env_update BEFORE UPDATE ON public.mb_env FOR EACH ROW EXECUTE PROCEDURE public.compute_env_state_before_env_update();


--
-- Name: mb_env_inbound update_env_state_after_inbound_delete; Type: TRIGGER; Schema: public; Owner: -
--
--	Changed by KAG 2023-12-07 from 'EXECUTE FUNCTION' to 'EXECUTE PROCEDURE' to ensure creation on PostgreSQL 10.x

CREATE TRIGGER update_env_state_after_inbound_delete AFTER DELETE ON public.mb_env_inbound FOR EACH ROW EXECUTE PROCEDURE public.update_env_state_after_inbound_delete();


--
-- Name: mb_env_inbound update_env_state_after_inbound_insert_update; Type: TRIGGER; Schema: public; Owner: -
--
--	Changed by KAG 2023-12-07 from 'EXECUTE FUNCTION' to 'EXECUTE PROCEDURE' to ensure creation on PostgreSQL 10.x

CREATE TRIGGER update_env_state_after_inbound_insert_update AFTER INSERT OR UPDATE ON public.mb_env_inbound FOR EACH ROW EXECUTE PROCEDURE public.update_env_state_after_inbound_insert_update();


--
-- Name: mb_configuration update_env_state_after_trigger_delete; Type: TRIGGER; Schema: public; Owner: -
--
--	Changed by KAG 2023-12-07 from 'EXECUTE FUNCTION' to 'EXECUTE PROCEDURE' to ensure creation on PostgreSQL 10.x

CREATE TRIGGER update_env_state_after_trigger_delete AFTER DELETE ON public.mb_configuration FOR EACH ROW WHEN (((old.type)::text = 'trigger'::text)) EXECUTE PROCEDURE public.update_env_state_after_trigger_delete();


--
-- Name: mb_configuration update_env_state_after_trigger_insert; Type: TRIGGER; Schema: public; Owner: -
--
--	Changed by KAG 2023-12-07 from 'EXECUTE FUNCTION' to 'EXECUTE PROCEDURE' to ensure creation on PostgreSQL 10.x

CREATE TRIGGER update_env_state_after_trigger_insert AFTER INSERT ON public.mb_configuration FOR EACH ROW WHEN (((new.type)::text = 'trigger'::text)) EXECUTE PROCEDURE public.update_env_state_after_trigger_insert();


--
-- Name: mb_configuration update_env_state_after_trigger_update; Type: TRIGGER; Schema: public; Owner: -
--
--	Changed by KAG 2023-12-07 from 'EXECUTE FUNCTION' to 'EXECUTE PROCEDURE' to ensure creation on PostgreSQL 10.x

CREATE TRIGGER update_env_state_after_trigger_update AFTER UPDATE OF trigger_state ON public.mb_configuration FOR EACH ROW WHEN (((old.type)::text = 'trigger'::text)) EXECUTE PROCEDURE public.update_env_state_after_trigger_update();


--
-- Name: mb_chain_labels fk1l896tl9erhwkm7b5y2tlj6tr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_chain_labels
    ADD CONSTRAINT fk1l896tl9erhwkm7b5y2tlj6tr FOREIGN KEY (id) REFERENCES public.mb_chain(id);


--
-- Name: mb_message_param_multiple_value fk1pmedorbtjrcgg3llab9klbnp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_param_multiple_value
    ADD CONSTRAINT fk1pmedorbtjrcgg3llab9klbnp FOREIGN KEY (message_param_id) REFERENCES public.mb_message_param(id);


--
-- Name: mb_end_situations fk284cymkgriieeguxra7qe1o26; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_end_situations
    ADD CONSTRAINT fk284cymkgriieeguxra7qe1o26 FOREIGN KEY (situation_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_servers fk2qxx3ak03035pasa8b7bwgiu6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_servers
    ADD CONSTRAINT fk2qxx3ak03035pasa8b7bwgiu6 FOREIGN KEY (parent_id) REFERENCES public.mb_folders(id);


--
-- Name: mb_configuration fk2u1voqn378t6r6qddodiqca13; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fk2u1voqn378t6r6qddodiqca13 FOREIGN KEY (parent_conf_id) REFERENCES public.mb_configuration(id);


--
-- Name: mb_steps fk3atoy0ktiincco3rjy2loisg3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fk3atoy0ktiincco3rjy2loisg3 FOREIGN KEY (receiver_id) REFERENCES public.mb_systems(id);


--
-- Name: mb_condition_props fk42rkh13qxtoa2f0rqnrhlcy09; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_condition_props
    ADD CONSTRAINT fk42rkh13qxtoa2f0rqnrhlcy09 FOREIGN KEY (step_id) REFERENCES public.mb_steps(id);


--
-- Name: mb_operations fk5wubx0ib047fyputrpmfwrvhx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_operations
    ADD CONSTRAINT fk5wubx0ib047fyputrpmfwrvhx FOREIGN KEY (default_inbound_situation) REFERENCES public.mb_situation(id);


--
-- Name: mb_message_param fk60bxcsjq5gyf1aa1hsfhtpa6h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_param
    ADD CONSTRAINT fk60bxcsjq5gyf1aa1hsfhtpa6h FOREIGN KEY (context_id) REFERENCES public.mb_context(id);


--
-- Name: mb_message_headers fk626dd402mg01ptuapqvgf8r2d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_headers
    ADD CONSTRAINT fk626dd402mg01ptuapqvgf8r2d FOREIGN KEY (parent_id) REFERENCES public.mb_message(id);


--
-- Name: mb_message_connection_properties fk66j5wrhx1v2fj17kqooweh376; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_connection_properties
    ADD CONSTRAINT fk66j5wrhx1v2fj17kqooweh376 FOREIGN KEY (parent_id) REFERENCES public.mb_message(id);


--
-- Name: mb_configuration fk68m6ql3lyjbb7cneey9vh7aem; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fk68m6ql3lyjbb7cneey9vh7aem FOREIGN KEY (interceptor_id) REFERENCES public.mb_interceptors(id);


--
-- Name: mb_steps fk6caliedh53831bs2gp0hhuxq7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fk6caliedh53831bs2gp0hhuxq7 FOREIGN KEY (operation_id) REFERENCES public.mb_operations(id);


--
-- Name: mb_steps fk6hebsnr90o1heepvau5d8uj3a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fk6hebsnr90o1heepvau5d8uj3a FOREIGN KEY (op_template_id) REFERENCES public.mb_templates(id);


--
-- Name: mb_steps_keys_to_regenerate fk6j7gc3ehreyq74x6nl1x3ne3w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps_keys_to_regenerate
    ADD CONSTRAINT fk6j7gc3ehreyq74x6nl1x3ne3w FOREIGN KEY (parent_id) REFERENCES public.mb_steps(id);


--
-- Name: mb_context fk6wgio1ntmdaced8qk0tsj4r54; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fk6wgio1ntmdaced8qk0tsj4r54 FOREIGN KEY (outgoing_message_id) REFERENCES public.mb_message(id);


--
-- Name: mb_situation_keys_to_regenerate fk76mk21ax1ef15g0jq2lkekbum; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situation_keys_to_regenerate
    ADD CONSTRAINT fk76mk21ax1ef15g0jq2lkekbum FOREIGN KEY (parent_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_situations_parsingrules fk78u1bucdphhb54tfr1cfgwjfo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situations_parsingrules
    ADD CONSTRAINT fk78u1bucdphhb54tfr1cfgwjfo FOREIGN KEY (parsingrule_id) REFERENCES public.mb_parsing_rules(id);


--
-- Name: mb_configuration fk7h5xvxg7kn8km3b866bf9a8ib; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fk7h5xvxg7kn8km3b866bf9a8ib FOREIGN KEY (parent_project_id) REFERENCES public.mb_projects(id);


--
-- Name: mb_env_inbound fk87jqhj0920jajedteeusfelr8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_inbound
    ADD CONSTRAINT fk87jqhj0920jajedteeusfelr8 FOREIGN KEY (environment_id) REFERENCES public.mb_env(id);


--
-- Name: mb_configuration fk8m91q9284fq8jt3jl7r57yi9m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fk8m91q9284fq8jt3jl7r57yi9m FOREIGN KEY (parent_template_id) REFERENCES public.mb_templates(id);


--
-- Name: mb_operations fk8rj48e7pjafsr1gjwu7rq3adg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_operations
    ADD CONSTRAINT fk8rj48e7pjafsr1gjwu7rq3adg FOREIGN KEY (transport_id) REFERENCES public.mb_configuration(id);


--
-- Name: mb_folders fk96mgmms8y3ealfqkrfggb814j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_folders
    ADD CONSTRAINT fk96mgmms8y3ealfqkrfggb814j FOREIGN KEY (project_id) REFERENCES public.mb_projects(id) ON DELETE CASCADE;


--
-- Name: mb_condition_props fk96t0o93ubr1ubysysqtulxgfh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_condition_props
    ADD CONSTRAINT fk96t0o93ubr1ubysysqtulxgfh FOREIGN KEY (trigger_id) REFERENCES public.mb_triggers(id);


--
-- Name: mb_env_outbound fk9ny206rrjkylsqkmhkbppniqb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_outbound
    ADD CONSTRAINT fk9ny206rrjkylsqkmhkbppniqb FOREIGN KEY (systems) REFERENCES public.mb_systems(id);


--
-- Name: mb_templates fk9v82wlqsbu1w7c6hnnhfvtg7q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_templates
    ADD CONSTRAINT fk9v82wlqsbu1w7c6hnnhfvtg7q FOREIGN KEY (parent_system_id) REFERENCES public.mb_systems(id);


--
-- Name: mb_exceptional_situation fk9x0dsdoe7jywxvo2h375xu5ba; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_exceptional_situation
    ADD CONSTRAINT fk9x0dsdoe7jywxvo2h375xu5ba FOREIGN KEY (situation_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_configuration fk_i8uw39bedn18j0ryhtp79hw4g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fk_i8uw39bedn18j0ryhtp79hw4g FOREIGN KEY (system_id) REFERENCES public.mb_systems(id) ON DELETE CASCADE;


--
-- Name: mb_folders fkaivahng06sscc4l1st8cawrme; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_folders
    ADD CONSTRAINT fkaivahng06sscc4l1st8cawrme FOREIGN KEY (parent_id) REFERENCES public.mb_folders(id);


--
-- Name: mb_configuration fkajmgw9529clctpxoy3rm52jva; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkajmgw9529clctpxoy3rm52jva FOREIGN KEY (applicability_params_interceptor_id) REFERENCES public.mb_interceptors(id);


--
-- Name: mb_configuration fkawcdns0j6wskwq4h1n470lnke; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkawcdns0j6wskwq4h1n470lnke FOREIGN KEY (parent_out_server_id) REFERENCES public.mb_servers(id);


--
-- Name: mb_situations_parsingrules fkb781axa2k4op0i7npqyen9q0a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situations_parsingrules
    ADD CONSTRAINT fkb781axa2k4op0i7npqyen9q0a FOREIGN KEY (situation_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_chain_keys fkb8mxu4sfl2y82xkcpheavn79; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_chain_keys
    ADD CONSTRAINT fkb8mxu4sfl2y82xkcpheavn79 FOREIGN KEY (parent_id) REFERENCES public.mb_chain(id);


--
-- Name: mb_triggers fkbddj3vt6j3hfq7cdfe56e098q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_triggers
    ADD CONSTRAINT fkbddj3vt6j3hfq7cdfe56e098q FOREIGN KEY (set_parent_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_project_settings fkc3sk06mbowtfyu6q8a14dgbdg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_project_settings
    ADD CONSTRAINT fkc3sk06mbowtfyu6q8a14dgbdg FOREIGN KEY (project_id) REFERENCES public.mb_projects(id) ON DELETE CASCADE;


--
-- Name: mb_context fkc3yxgi5y361onoqo2gaqufwuf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fkc3yxgi5y361onoqo2gaqufwuf FOREIGN KEY (instance) REFERENCES public.mb_instance(id);


--
-- Name: mb_end_situations fkca3t30jj7ywi06trsdxnwnv7i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_end_situations
    ADD CONSTRAINT fkca3t30jj7ywi06trsdxnwnv7i FOREIGN KEY (step_id) REFERENCES public.mb_steps(id);


--
-- Name: mb_chain fkceb8wwjsvvcwsdhr6ejfcnoe3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_chain
    ADD CONSTRAINT fkceb8wwjsvvcwsdhr6ejfcnoe3 FOREIGN KEY (id) REFERENCES public.mb_step_container(id);


--
-- Name: mb_configuration fkd3sb0cwbxnypsa061pvm2g0ki; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkd3sb0cwbxnypsa061pvm2g0ki FOREIGN KEY (ei_session_parent_id) REFERENCES public.mb_projects(id);


--
-- Name: mb_compatible_ds_lists fke1exk5ymed48pp9deifyr47s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_compatible_ds_lists
    ADD CONSTRAINT fke1exk5ymed48pp9deifyr47s FOREIGN KEY (callchain_id) REFERENCES public.mb_chain(id);


--
-- Name: mb_context fke2o6hcnrgh1prihqfxb59bu3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fke2o6hcnrgh1prihqfxb59bu3 FOREIGN KEY (initiator_id) REFERENCES public.mb_instance(id);


--
-- Name: mb_situation_labels fkeddofjthpv33sdvhb035con05; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situation_labels
    ADD CONSTRAINT fkeddofjthpv33sdvhb035con05 FOREIGN KEY (id) REFERENCES public.mb_situation(id);


--
-- Name: mb_steps fkee1s38bcejwkpxm4a7l8dkxl0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fkee1s38bcejwkpxm4a7l8dkxl0 FOREIGN KEY (sender_id) REFERENCES public.mb_systems(id);


--
-- Name: mb_operations fkejaogomweor0qfaob2rohtswl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_operations
    ADD CONSTRAINT fkejaogomweor0qfaob2rohtswl FOREIGN KEY (parent_id) REFERENCES public.mb_systems(id);


--
-- Name: mb_triggers fkekdm8encco1g9kbmnb0cwj6wq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_triggers
    ADD CONSTRAINT fkekdm8encco1g9kbmnb0cwj6wq FOREIGN KEY (oet_parent_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_templates fkf3h5qy0d5q69pl6b6k0mkn9nr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_templates
    ADD CONSTRAINT fkf3h5qy0d5q69pl6b6k0mkn9nr FOREIGN KEY (parent_operation_id) REFERENCES public.mb_operations(id);


--
-- Name: mb_compatible_with_transports fkf635bg4kwymiovew7sbgkdi1l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_compatible_with_transports
    ADD CONSTRAINT fkf635bg4kwymiovew7sbgkdi1l FOREIGN KEY (parent_id) REFERENCES public.mb_templates(id);


--
-- Name: mb_steps fkfnp33rpwi61ps6l74xymwbyrn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fkfnp33rpwi61ps6l74xymwbyrn FOREIGN KEY (sys_template_id) REFERENCES public.mb_templates(id);


--
-- Name: mb_projects fkftcigom8bl62eeayvjfvhagf7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_projects
    ADD CONSTRAINT fkftcigom8bl62eeayvjfvhagf7 FOREIGN KEY (chains_folder) REFERENCES public.mb_folders(id) ON DELETE CASCADE;


--
-- Name: mb_chain fkfxxhtajsrxs5tg9vbkthay0v7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_chain
    ADD CONSTRAINT fkfxxhtajsrxs5tg9vbkthay0v7 FOREIGN KEY (parent_id) REFERENCES public.mb_folders(id);


--
-- Name: mb_instance fkgahimqlhpchl2gkqk5fxld7vm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_instance
    ADD CONSTRAINT fkgahimqlhpchl2gkqk5fxld7vm FOREIGN KEY (context_id) REFERENCES public.mb_context(id);


--
-- Name: mb_parsing_rules fkgv6drxks2whnafheipf39yly2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_parsing_rules
    ADD CONSTRAINT fkgv6drxks2whnafheipf39yly2 FOREIGN KEY (parent_system_id) REFERENCES public.mb_systems(id);


--
-- Name: mb_env_inbound fkh9wiv6tyrr56x4xi3xm1otufu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_inbound
    ADD CONSTRAINT fkh9wiv6tyrr56x4xi3xm1otufu FOREIGN KEY (servers) REFERENCES public.mb_servers(id);


--
-- Name: mb_env_outbound fkhbofslcr1ukpe0ptxpbjuquqg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_outbound
    ADD CONSTRAINT fkhbofslcr1ukpe0ptxpbjuquqg FOREIGN KEY (servers) REFERENCES public.mb_servers(id);


--
-- Name: mb_context fkhcdfgv3kvgpxjkc6dnij0ukxx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fkhcdfgv3kvgpxjkc6dnij0ukxx FOREIGN KEY (parent_ctx_id) REFERENCES public.mb_context(id);


--
-- Name: mb_operations fkhkaomak0ycuynyijcc5mi0nnp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_operations
    ADD CONSTRAINT fkhkaomak0ycuynyijcc5mi0nnp FOREIGN KEY (error_inbound_situation) REFERENCES public.mb_situation(id);


--
-- Name: mb_configuration fkhl3eeauk28wuie48snnr4of9x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkhl3eeauk28wuie48snnr4of9x FOREIGN KEY (parent_in_server_id) REFERENCES public.mb_servers(id);


--
-- Name: mb_instance fkhy871vsc31yc5i9709x8dhkoh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_instance
    ADD CONSTRAINT fkhy871vsc31yc5i9709x8dhkoh FOREIGN KEY (parent_id) REFERENCES public.mb_instance(id);


--
-- Name: mb_env_inbound fki0b8iolku16l0qd72fhaohc3r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_inbound
    ADD CONSTRAINT fki0b8iolku16l0qd72fhaohc3r FOREIGN KEY (systems) REFERENCES public.mb_systems(id);


--
-- Name: mb_context fki928mvh0tqg02ps4wnh93y94r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fki928mvh0tqg02ps4wnh93y94r FOREIGN KEY (incoming_message_id) REFERENCES public.mb_message(id);


--
-- Name: mb_folders_labels fkiet4akl1hbxncqgxu6xi16lbe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_folders_labels
    ADD CONSTRAINT fkiet4akl1hbxncqgxu6xi16lbe FOREIGN KEY (id) REFERENCES public.mb_folders(id);


--
-- Name: mb_projects fkixbcs3mn214168bt5fimhjpyu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_projects
    ADD CONSTRAINT fkixbcs3mn214168bt5fimhjpyu FOREIGN KEY (systems_folder) REFERENCES public.mb_folders(id) ON DELETE CASCADE;


--
-- Name: mb_context_binding_keys fkja6gfusmbwqxk1jr9cbh3jbto; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_binding_keys
    ADD CONSTRAINT fkja6gfusmbwqxk1jr9cbh3jbto FOREIGN KEY (id) REFERENCES public.mb_context(id);


--
-- Name: mb_context fkkfmmr0m8o82rkas3dt2mcrmla; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fkkfmmr0m8o82rkas3dt2mcrmla FOREIGN KEY (step_id) REFERENCES public.mb_instance(id);


--
-- Name: mb_configuration fkki7k8qfon0dyeqrlubyi9g8ls; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkki7k8qfon0dyeqrlubyi9g8ls FOREIGN KEY (parent_env_id) REFERENCES public.mb_env(id);


--
-- Name: mb_headers fkkqymjv8rqqjodwmfs6dp12wvh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_headers
    ADD CONSTRAINT fkkqymjv8rqqjodwmfs6dp12wvh FOREIGN KEY (parent_id) REFERENCES public.mb_templates(id);


--
-- Name: mb_steps fkleqcyrt3v2bsge3h14tp2bfc5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fkleqcyrt3v2bsge3h14tp2bfc5 FOREIGN KEY (situation_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_counter_owners fkmivd6f9fy5fpvv0wdxax2ij4o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_counter_owners
    ADD CONSTRAINT fkmivd6f9fy5fpvv0wdxax2ij4o FOREIGN KEY (counter_id) REFERENCES public.mb_counter(id);


--
-- Name: mb_env fkn5iwup00y7d3d3btb1lee5x1m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env
    ADD CONSTRAINT fkn5iwup00y7d3d3btb1lee5x1m FOREIGN KEY (parent_id) REFERENCES public.mb_folders(id);


--
-- Name: mb_templates_labels fknaisj5dyun9oyfpy76jgrw3rt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_templates_labels
    ADD CONSTRAINT fknaisj5dyun9oyfpy76jgrw3rt FOREIGN KEY (id) REFERENCES public.mb_templates(id);


--
-- Name: mb_situation fkouq9xiqke337oi0bim1slagxx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situation
    ADD CONSTRAINT fkouq9xiqke337oi0bim1slagxx FOREIGN KEY (parent_id) REFERENCES public.mb_operations(id);


--
-- Name: mb_interceptors fkp42f5obtmq32pa24mcr3sy1tu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_interceptors
    ADD CONSTRAINT fkp42f5obtmq32pa24mcr3sy1tu FOREIGN KEY (parent_template_id) REFERENCES public.mb_templates(id);


--
-- Name: mb_systems_labels fkp73esl5kbrbrshx19nh9ovprs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_systems_labels
    ADD CONSTRAINT fkp73esl5kbrbrshx19nh9ovprs FOREIGN KEY (id) REFERENCES public.mb_systems(id);


--
-- Name: mb_exceptional_situation fkpgdv4amrijtgqpilgmle2ea9a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_exceptional_situation
    ADD CONSTRAINT fkpgdv4amrijtgqpilgmle2ea9a FOREIGN KEY (step_id) REFERENCES public.mb_steps(id);


--
-- Name: mb_projects fkpn8i2nwlogg6xqeqa5cwonehx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_projects
    ADD CONSTRAINT fkpn8i2nwlogg6xqeqa5cwonehx FOREIGN KEY (servers_folder) REFERENCES public.mb_folders(id) ON DELETE CASCADE;


--
-- Name: mb_context_report_links fkpxghq2qbd7ty68l237a1dpm4h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_report_links
    ADD CONSTRAINT fkpxghq2qbd7ty68l237a1dpm4h FOREIGN KEY (parent_id) REFERENCES public.mb_context(id);


--
-- Name: mb_triggers fkql520akex4v76eowsfo0ae45u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_triggers
    ADD CONSTRAINT fkql520akex4v76eowsfo0ae45u FOREIGN KEY (situation_id) REFERENCES public.mb_situation(id);


--
-- Name: mb_configuration fkqt7o5o2u5mhjv06sq4xg4u3bm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkqt7o5o2u5mhjv06sq4xg4u3bm FOREIGN KEY (system_id) REFERENCES public.mb_systems(id);


--
-- Name: mb_projects fkr40fgbxlr3598xgj44g0a0u4e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_projects
    ADD CONSTRAINT fkr40fgbxlr3598xgj44g0a0u4e FOREIGN KEY (envs_folder) REFERENCES public.mb_folders(id) ON DELETE CASCADE;


--
-- Name: mb_configuration fks38l0i2mtggc33r84cklcki7l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fks38l0i2mtggc33r84cklcki7l FOREIGN KEY (transport_id) REFERENCES public.mb_configuration(id);


--
-- Name: mb_systems fks3xjj4whiorrdf61egjriokeh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_systems
    ADD CONSTRAINT fks3xjj4whiorrdf61egjriokeh FOREIGN KEY (parent_id) REFERENCES public.mb_folders(id);


--
-- Name: mb_context fkt1kwr7x3ianqawq5l94nk9jsr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT fkt1kwr7x3ianqawq5l94nk9jsr FOREIGN KEY (tc_id) REFERENCES public.mb_context(id);


--
-- Name: mb_bv_cases fkt4y8l5kb1m2bnyd10dx8iesto; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_bv_cases
    ADD CONSTRAINT fkt4y8l5kb1m2bnyd10dx8iesto FOREIGN KEY (callchain_id) REFERENCES public.mb_chain(id);


--
-- Name: mb_interceptors fkt5mx8js595sh3v822hsvtp5lg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_interceptors
    ADD CONSTRAINT fkt5mx8js595sh3v822hsvtp5lg FOREIGN KEY (parent_transport_id) REFERENCES public.mb_configuration(id);


--
-- Name: mb_steps fktd7midybhdg18l1v54ywveaqh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fktd7midybhdg18l1v54ywveaqh FOREIGN KEY (chain_id) REFERENCES public.mb_chain(id);


--
-- Name: mb_env_outbound fkteeero957bk0mkpi1j8hopuo7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_env_outbound
    ADD CONSTRAINT fkteeero957bk0mkpi1j8hopuo7 FOREIGN KEY (environment_id) REFERENCES public.mb_env(id);


--
-- Name: mb_steps fktfyxjk6r50kuj9314rxepuwd5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_steps
    ADD CONSTRAINT fktfyxjk6r50kuj9314rxepuwd5 FOREIGN KEY (parent_id) REFERENCES public.mb_step_container(id);


--
-- Name: mb_parsing_rules fktg3ws7ksm2frqg4aybncffvjv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_parsing_rules
    ADD CONSTRAINT fktg3ws7ksm2frqg4aybncffvjv FOREIGN KEY (parent_operation_id) REFERENCES public.mb_operations(id);


--
-- Name: mb_situation fkthrt3p8mjqu2r8714whk3iaw8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_situation
    ADD CONSTRAINT fkthrt3p8mjqu2r8714whk3iaw8 FOREIGN KEY (id) REFERENCES public.mb_step_container(id);


--
-- Name: mb_configuration fkti8jpqgjmkhjjg0vk4kfw3tsu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_configuration
    ADD CONSTRAINT fkti8jpqgjmkhjjg0vk4kfw3tsu FOREIGN KEY (parent_system_id) REFERENCES public.mb_systems(id);


--
-- PostgreSQL database dump complete
--

