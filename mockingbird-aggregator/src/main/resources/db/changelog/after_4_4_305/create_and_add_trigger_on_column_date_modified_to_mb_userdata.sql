CREATE OR REPLACE FUNCTION compute_date_modified_before_userdata_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    new.date_modified := now();
RETURN NEW;
END;
$$;
commit;

CREATE TRIGGER compute_date_modified_before_userdata_update BEFORE UPDATE ON mb_userdata
    FOR EACH ROW EXECUTE PROCEDURE compute_date_modified_before_userdata_update();
commit;
