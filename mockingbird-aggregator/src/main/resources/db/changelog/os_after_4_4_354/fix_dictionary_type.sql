update mb_configuration
    set params = jsonb_set(params::jsonb, '{dictionary type}', '"Standard"')
where
    type_name like '%automation.itf.transport.diameter.outbound.DiameterOutbound%'
    and params like '%dictionary type%'
    and ((params::jsonb)->'dictionary type')::text like '"% Diameter"';

commit;