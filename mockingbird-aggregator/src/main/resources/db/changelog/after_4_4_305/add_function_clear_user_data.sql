CREATE OR REPLACE FUNCTION clear_user_data_func(leave_days integer) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN
    delete from mb_userdata where date_modified <= (now() - make_interval(days => leave_days));
RETURN 'Success;';
END;
$$;
commit;
