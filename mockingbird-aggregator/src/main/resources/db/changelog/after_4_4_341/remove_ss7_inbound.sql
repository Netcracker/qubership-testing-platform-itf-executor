delete from mb_configuration
  where type = 'trigger' and type_name like '%.automation.itf.transport.ss7.inbound.SS7InboundTransport';
commit;
delete from mb_configuration
  where type = 'inbound' and type_name like '%.automation.itf.transport.ss7.inbound.SS7InboundTransport';
commit;
delete from mb_configuration
  where type = 'template' and type_name like '%.automation.itf.transport.ss7.inbound.SS7InboundTransport';
commit;
delete from mb_configuration
  where type = 'transport' and type_name like '%.automation.itf.transport.ss7.inbound.SS7InboundTransport';
commit;
