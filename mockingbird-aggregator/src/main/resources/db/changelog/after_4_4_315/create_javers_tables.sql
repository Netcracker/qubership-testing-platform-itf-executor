--
-- PostgreSQL database dump
--

-- Dumped from database version 14.8
-- Dumped by pg_dump version 14.8

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
-- Name: jv_commit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jv_commit (
                                  commit_pk bigint NOT NULL,
                                  author character varying(200),
                                  commit_date timestamp without time zone,
                                  commit_date_instant character varying(30),
                                  commit_id numeric(22,2)
);


--
-- Name: jv_commit_pk_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.jv_commit_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jv_commit_property; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jv_commit_property (
                                           property_name character varying(191) NOT NULL,
                                           property_value character varying(600),
                                           commit_fk bigint NOT NULL
);


--
-- Name: jv_global_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jv_global_id (
                                     global_id_pk bigint NOT NULL,
                                     local_id character varying(191),
                                     fragment character varying(200),
                                     type_name character varying(200),
                                     owner_id_fk bigint
);


--
-- Name: jv_global_id_pk_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.jv_global_id_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jv_snapshot; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jv_snapshot (
                                    snapshot_pk bigint NOT NULL,
                                    type character varying(200),
                                    version bigint,
                                    state text,
                                    changed_properties text,
                                    managed_type character varying(200),
                                    global_id_fk bigint,
                                    commit_fk bigint
);


--
-- Name: jv_snapshot_pk_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.jv_snapshot_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jv_commit jv_commit_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_commit
    ADD CONSTRAINT jv_commit_pk PRIMARY KEY (commit_pk);


--
-- Name: jv_commit_property jv_commit_property_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_commit_property
    ADD CONSTRAINT jv_commit_property_pk PRIMARY KEY (commit_fk, property_name);


--
-- Name: jv_global_id jv_global_id_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_global_id
    ADD CONSTRAINT jv_global_id_pk PRIMARY KEY (global_id_pk);


--
-- Name: jv_snapshot jv_snapshot_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_snapshot
    ADD CONSTRAINT jv_snapshot_pk PRIMARY KEY (snapshot_pk);


--
-- Name: jv_commit_commit_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_commit_commit_id_idx ON public.jv_commit USING btree (commit_id);


--
-- Name: jv_commit_property_commit_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_commit_property_commit_fk_idx ON public.jv_commit_property USING btree (commit_fk);


--
-- Name: jv_commit_property_property_name_property_value_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_commit_property_property_name_property_value_idx ON public.jv_commit_property USING btree (property_name, property_value);


--
-- Name: jv_global_id_local_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_global_id_local_id_idx ON public.jv_global_id USING btree (local_id);


--
-- Name: jv_global_id_owner_id_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_global_id_owner_id_fk_idx ON public.jv_global_id USING btree (owner_id_fk);


--
-- Name: jv_snapshot_commit_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_snapshot_commit_fk_idx ON public.jv_snapshot USING btree (commit_fk);


--
-- Name: jv_snapshot_global_id_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jv_snapshot_global_id_fk_idx ON public.jv_snapshot USING btree (global_id_fk);


--
-- Name: jv_commit_property jv_commit_property_commit_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_commit_property
    ADD CONSTRAINT jv_commit_property_commit_fk FOREIGN KEY (commit_fk) REFERENCES public.jv_commit(commit_pk);


--
-- Name: jv_global_id jv_global_id_owner_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_global_id
    ADD CONSTRAINT jv_global_id_owner_id_fk FOREIGN KEY (owner_id_fk) REFERENCES public.jv_global_id(global_id_pk);


--
-- Name: jv_snapshot jv_snapshot_commit_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_snapshot
    ADD CONSTRAINT jv_snapshot_commit_fk FOREIGN KEY (commit_fk) REFERENCES public.jv_commit(commit_pk);


--
-- Name: jv_snapshot jv_snapshot_global_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jv_snapshot
    ADD CONSTRAINT jv_snapshot_global_id_fk FOREIGN KEY (global_id_fk) REFERENCES public.jv_global_id(global_id_pk);


--
-- PostgreSQL database dump complete
--

