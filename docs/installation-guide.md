# Qubership ATP-ITF-EXECUTOR Installation Guide

## 3rd party dependencies

| Name       | Version | Mandatory/Optional | Comment                |
|------------|---------|--------------------|------------------------|
| PostgreSQL | 14+     | Mandatory          | JDBC connection string |
| GridFS     | 4.2+    | Mandatory          | For storing files      |

## HWE

|                  | CPU request | CPU limit | RAM request | RAM limit |
|------------------|-------------|-----------|-------------|-----------|
| Dev level        | 50m         | 500m      | 300Mi       | 1500Mi    |
| Production level | 50m         | 1500m     | 3Gi         | 3Gi       |

## Minimal parameters set

```properties
-DJDBC_URL=
-DITF_EXECUTOR_DB_USER=
-DITF_EXECUTOR_DB_PASSWORD=
-DHTTP_PORT=
-DKEYSTORE_PASSWORD=
-DSPRING_PROFILES=
-DKEYCLOAK_ENABLED=
-DKEYCLOAK_CLIENT_NAME=
-DKEYCLOAK_SECRET=
-DKEYCLOAK_REALM=
-DKEYCLOAK_AUTH_URL=
-DATP_ITF_BROKER_URL_TCP=
-DATP_ITF_BROKER_URL_WS=
-DCONFIGURATOR_EXECUTOR_EVENT_TRIGGERS_TOPIC=
-DEXECUTOR_CONFIGURATOR_EVENT_TRIGGERS_TOPIC=
-DEXECUTOR_STUBS_SYNC_TOPIC=
-DCONFIGURATOR_STUBS_TOPIC=
-DSTUBS_CONFIGURATOR_TOPIC=
-DEND_EXCEPTIONAL_SITUATIONS_EVENTS_TOPIC=
-DEXECUTOR_TCCONTEXT_OPERATIONS_TOPIC=
-DEXECUTOR_DISABLING_STEPBYSTEP_TOPIC=
-DEXECUTOR_SYNC_RELOAD_DICTIONARY_TOPIC=
-DEDS_UPDATE_TOPIC=
-DSTUBS_EXECUTOR_INCOMING_QUEUE=
-DEXECUTOR_STUBS_OUTGOING_QUEUE=
-DREPORT_QUEUE=
```

### Full ENV VARs list per container

| Deploy Parameter Name                                  | Mandatory | Example                                                                                                        | Description                                                     |
|--------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `JDBC_URL`                                             | Yes       | jdbc:postgresql://localhost:5432/reporting                                                                     | Datasource url                                                  |
| `ITF_EXECUTOR_DB_USER`                                 | Yes       | postgres                                                                                                       | Datasource username                                             |
| `ITF_EXECUTOR_DB_PASSWORD`                             | Yes       | postgres                                                                                                       | Datasource password                                             |
| `SPRING_DATASOURCE_MINIDLE`                            | No        | 10                                                                                                             | Spring hikari minimum-idle value                                |
| `SPRING_DATASOURCE_MAXTOTAL`                           | No        | 2000                                                                                                           | Spring hikari maximum-pool-size value                           |
| `SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT`                | No        | 55000                                                                                                          | Spring hikari idle-timeout value                                |
| `SPRING_DATASOURCE_HIKARI_MAX_LIFETIME`                | No        | 7200000                                                                                                        | Spring hikari max-lifetime value                                |
| `HTTP_PORT`                                            | Yes       | 8080                                                                                                           | Server port number                                              |
| `EMBEDDED_HTTPS_ENABLED`                               | No        | true                                                                                                           | Enable or disable support https                                 |
| `EMBEDDED_TLS_SERVER_PORT`                             | No        | 9443                                                                                                           | List tls server port number                                     | 
| `EMBEDDED_SSL_SERVER_PORT`                             | No        | 8443                                                                                                           | List ssl server port numbers                                    |
| `KEYSTORE_PASSWORD`                                    | Yes       | changeit                                                                                                       | Keystore password value                                         |
| `UNDERTOW_SESSION_TIMEOUT`                             | No        | 58m                                                                                                            | Server servlet session timeout value                            |
| `UNDERTOW_COMPRESSION_ENABLED`                         | No        | false                                                                                                          | Enable or disable undertow server compression                   |
| `UNDERTOW_COMPRESSION_MIMETYPE`                        | No        | text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml | Undertow server compression mime-types value                    |
| `SERVER_UNDERTOW_IO_THREADS`                           | No        | 4                                                                                                              | Undertow server threads io number                               |
| `SERVER_UNDERTOW_WORKER_THREADS`                       | No        | 32                                                                                                             | Undertow server threads worker number                           |
| `SERVER_UNDERTOW_ACCESSLOG_ENABLED`                    | No        | true                                                                                                           | Enable or disable undertow server accesslog                     |
| `JBOSS_THREADS_EQE_STATISTICS`                         | No        | true                                                                                                           | Jboss threads statistics                                        |
| `MAX_RAM_SIZE`                                         | No        | 2560m                                                                                                          | Max ram size value                                              |
| `JPDA_ENABLED`                                         | No        | false                                                                                                          | Enable or disable jpda                                          |
| `LOG_LEVEL`                                            | No        | INFO                                                                                                           | Logging level value                                             |
| `GRAYLOG_ON`                                           | No        | false                                                                                                          | Enable or disable Graylog integration                           |
| `GRAYLOG_HOST`                                         | No        | graylog.atp.user.managed.qubership.cloud                                                                       | Graylog log host address                                        |
| `GRAYLOG_PORT`                                         | No        | 12201                                                                                                          | Graylog port value                                              |
| `SERVICE_NAME`                                         | No        | atp-itf-reporting                                                                                              | Service system name                                             |
| `EUREKA_CLIENT_ENABLED`                                | No        | false                                                                                                          | Enable or disable eureka integration                            |
| `SERVICE_REGISTRY_URL`                                 | No        | http://atp-registry-service:8761/eureka                                                                        | Eureka serviceUrl defaultZone value                             |
| `ATP_SERVICE_PUBLIC`                                   | No        | true                                                                                                           | Enable or disable public integration with ATP                   |
| `ATP_SERVICE_INTERNAL`                                 | No        | true                                                                                                           | Enable or disable internal integration with ATP                 |
| `ATP_SERVICE_PATH`                                     | No        | /api/atp-itf-stubs/v1/**                                                                                       | Service path                                                    |
| `SPRING_PROFILES`                                      | Yes       | default                                                                                                        | Spring active profiles                                          |
| `KEYCLOAK_ENABLED`                                     | Yes       | false                                                                                                          | Enable or disable Keycloak integration                          |
| `KEYCLOAK_CLIENT_NAME`                                 | Yes       | atp2                                                                                                           | Keycloak resource name                                          |
| `KEYCLOAK_SECRET`                                      | Yes       | f3e17149-94d0-47ed-a5b7-744c332fdf66                                                                           | keycloak secret value                                           |
| `KEYCLOAK_REALM`                                       | Yes       | atp2                                                                                                           | Keycloak realm name                                             |
| `KEYCLOAK_AUTH_URL`                                    | Yes       | localhost                                                                                                      | Keycloak auth URL                                               |
| `PROJECT_INFO_ENDPOINT`                                | No        | /api/v1/users/projects                                                                                         | Project metadata API endpoint                                   |
| `CONTENT_SECURITY_POLICY`                              | No        | default-src 'self' 'unsafe-inline' *                                                                           | Security policy settings for frontend                           |
| `ATP_PUBLIC_GATEWAY_URL`                               | No        | http://atp-public-gateway-service-address                                                                      | Public gateway url                                              |
| `ATP_INTERNAL_GATEWAY_URL`                             | No        | http://atp-internal-gateway:8080                                                                               | Internal gateway url                                            |
| `ATP_INTERNAL_GATEWAY_ENABLED`                         | No        | false                                                                                                          | Enable or disable Internal gateway                              |
| `ATP_INTERNAL_GATEWAY_NAME`                            | No        | atp-internal-gateway                                                                                           | Internal gateway name                                           |
| `FEIGN_HTTPCLIENT_DISABLE_SSL`                         | No        | true                                                                                                           | Feign enable or disable ssl validation                          |
| `FEIGN_HTTPCLIENT_ENABLED`                             | No        | true                                                                                                           | Enable or disable feign                                         |
| `FEIGN_OKHTTP_ENABLED`                                 | No        | true                                                                                                           | Enable or disable feign okhttp                                  |
| `FEIGN_ATP_DATASETS_NAME`                              | No        | ATP-DATASETS                                                                                                   | Feign atp-dataset client name                                   |
| `FEIGN_ATP_DATASETS_URL`                               | No        | -                                                                                                              | Feign atp-dataset client url                                    |
| `FEIGN_ATP_DATASETS_ROUTE`                             | No        | api/atp-datasets/v1                                                                                            | Feign atp-dataset client route                                  |
| `FEIGN_ATP_BV_NAME`                                    | No        | ATP-BV                                                                                                         | Feign atp-bv client name                                        |
| `FEIGN_ATP_BV_URL`                                     | No        | -                                                                                                              | Feign atp-bv client url                                         |
| `FEIGN_ATP_BV_ROUTE`                                   | No        | api/bvtool/v1                                                                                                  | Feign atp-bv client route                                       |
| `FEIGN_ATP_ENVIRONMENTS_NAME`                          | No        | ATP-ENVIRONMENTS                                                                                               | Feign atp-environments client name                              |
| `FEIGN_ATP_ENVIRONMENTS_URL`                           | No        | -                                                                                                              | Feign atp-environments client url                               |
| `FEIGN_ATP_ENVIRONMENTS_ROUTE`                         | No        | api/atp-environments/v1                                                                                        | Feign atp-environments client route                             |
| `FEIGN_ATP_CATALOGUE_NAME`                             | No        | ATP-CATALOGUE                                                                                                  | Feign atp-catalogue client name                                 |
| `FEIGN_ATP_CATALOGUE_ROUTE`                            | No        | -                                                                                                              | Feign atp-catalogue client url                                  |
| `FEIGN_ATP_CATALOGUE_URL`                              | No        | api/atp-catalogue/v1                                                                                           | Feign atp-catalogue client route                                |
| `FEIGN_ATP_USERS_URL`                                  | No        | ATP-USERS-BACKEND                                                                                              | Feign atp-users-backend client name                             |
| `FEIGN_ATP_USERS_NAME`                                 | No        | -                                                                                                              | Feign atp-users-backend client url                              |
| `FEIGN_ATP_USERS_ROUTE`                                | No        | api/atp-users-backend/v1                                                                                       | Feign atp-users-backend client route                            |
| `FEIGN_ATP_ITF_EXECUTOR_NAME`                          | No        | ATP-ITF-EXECUTOR                                                                                               | Feign atp-itf-executor client name                              |
| `FEIGN_ATP_ITF_EXECUTOR_URL`                           | No        | -                                                                                                              | Feign atp-itf-executor client url                               |
| `FEIGN_ATP_ITF_EXECUTOR_ROUTE`                         | No        | api/atp-itf-executor/v1                                                                                        | Feign atp-itf-executor client route                             |
| `FEIGN_ATP_ITF_REPORTS_NAME`                           | No        | ATP-ITF-REPORTS                                                                                                | Feign atp-itf-executor client name                              |
| `FEIGN_ATP_ITF_REPORTS_URL`                            | No        | -                                                                                                              | Feign atp-itf-executor client url                               |
| `FEIGN_ATP_ITF_REPORTS_ROUTE`                          | No        | api/atp-itf-reports/v1                                                                                         | Feign atp-itf-executor client route                             |
| `FEIGN_ATP_ITF_EI_NAME`                                | No        | ATP-ITF-EI                                                                                                     | Feign atp export import client name                             |
| `FEIGN_ATP_ITF_EI_URL`                                 | No        | -                                                                                                              | Feign atp export import client url                              |
| `FEIGN_ATP_ITF_EI_ROUTE`                               | No        | api/atp-itf-ei/v1                                                                                              | Feign atp export import client route                            |
| `FEIGN_CONNECT_TIMEOUT`                                | No        | 160000000                                                                                                      | Feign client default connect timeout value                      |
| `FEIGN_READ_TIMEOUT`                                   | No        | 160000000                                                                                                      | Feign client default read timeout value                         |
| `EI_GRIDFS_DB_ADDR`                                    | No        | localhost                                                                                                      | Export import database host address                             |
| `EI_GRIDFS_DB_PORT`                                    | No        | 27017                                                                                                          | Export import database port number                              |
| `EI_GRIDFS_DB`                                         | No        | local_itf_gridfs                                                                                               | Export import database name                                     |
| `EI_GRIDFS_USER`                                       | No        | admin                                                                                                          | Export import database username value                           |
| `EI_GRIDFS_PASSWORD`                                   | No        | admin                                                                                                          | Export import database password value                           |
| `KAFKA_ENABLE`                                         | No        | true                                                                                                           | Enable or disable integration kafka                             |
| `NAKAFKA_TOPICME`                                      | No        | catalog_notification_topic                                                                                     | Kafka topic name                                                |
| `KAFKA_ITF_LITE_EXPORT_ITF_START_TOPIC`                | No        | itf_lite_to_itf_export_start                                                                                   | Kafka topic itf lite export start name                          |
| `KAFKA_ITF_LITE_EXPORT_ITF_FINISH_TOPIC`               | No        | itf_lite_to_itf_export_finish                                                                                  | Kafka topic itf lite export finish name                         |
| `KAFKA_ENVIRONMENTS_NOTIFICATION_TOPIC`                | No        | environments_notification_topic                                                                                | Kafka topic environments_notification name                      |
| `KAFKA_SERVICE_ENTITIES_TOPIC`                         | No        | service_entities                                                                                               | Kafka service entities topic name                               |
| `KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS`              | No        | 1                                                                                                              | Kafka service entities topic partitions value                   |
| `KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR`      | No        | 3                                                                                                              | Kafka service entities topic replicas value                     |
| `SERVICE_ENTITIES_MIGRATION_ENABLED`                   | No        | true                                                                                                           | Enable or disable service entities migration                    |
| `KAFKA_SERVERS`                                        | No        | kafka.kafka-cluster.svc:9092                                                                                   | Kafka bootstrap-servers                                         |
| `KAFKA_CLIENT_ID`                                      | No        | atp-itf-executor                                                                                               | Kafka consumer client-id name                                   |
| `KAFKA_GROUP_ID`                                       | No        | atp-itf-executor                                                                                               | Kafka consumer group-id name                                    |
| `ZIPKIN_ENABLE`                                        | No        | false                                                                                                          | Enable or disable Zipkin distributed tracing                    |
| `ZIPKIN_PROBABILITY`                                   | No        | 1.0                                                                                                            | Zipkin probability level                                        |
| `ZIPKIN_URL`                                           | No        | http://jaeger-app-collector.jaeger.svc:9411                                                                    | Zipkin host address                                             |
| `ATP_ITF_BROKER_URL_TCP`                               | Yes       | tcp://atp-activemq:61616?wireFormat.maxInactivityDuration=0&wireFormat.maxFrameSize=104857600                  | Broker url                                                      |
| `ATP_ITF_BROKER_URL_WS`                                | Yes       | ws://atp-activemq:61614                                                                                        | Broker url ws                                                   |
| `CONFIGURATOR_EXECUTOR_EVENT_TRIGGERS_TOPIC`           | Yes       | configurator_executor_event_triggers                                                                           | Configurator executor event triggers topic name                 |
| `EXECUTOR_CONFIGURATOR_EVENT_TRIGGERS_TOPIC`           | Yes       | executor_configurator_event_triggers                                                                           | Executor configurator event triggers topic name                 |
| `EXECUTOR_STUBS_SYNC_TOPIC`                            | Yes       | executor_stubs_sync                                                                                            | Executor stubs sync topic name                                  |
| `CONFIGURATOR_STUBS_TOPIC`                             | Yes       | configurator_stubs                                                                                             | Configurator stubs topic name                                   |
| `STUBS_CONFIGURATOR_TOPIC`                             | Yes       | stubs_configurator                                                                                             | Stubs configurator topic name                                   |
| `END_EXCEPTIONAL_SITUATIONS_EVENTS_TOPIC`              | Yes       | end_exceptional_situations_events                                                                              | End exceptional situations events topic name                    |
| `EXECUTOR_TCCONTEXT_OPERATIONS_TOPIC`                  | Yes       | executor_tccontext_operations                                                                                  | Executor tccontext operations topic name                        |
| `EXECUTOR_DISABLING_STEPBYSTEP_TOPIC`                  | Yes       | executor_disabling_stepbystep                                                                                  | Executor disabling stepbystep topic name                        |
| `EXECUTOR_SYNC_RELOAD_DICTIONARY_TOPIC`                | Yes       | executor_sync_reload_dictionary                                                                                | Executor sync reload dictionary topic name                      |
| `EDS_UPDATE_TOPIC`                                     | Yes       | eds_update                                                                                                     | Eds update topic name                                           |
| `STUBS_EXECUTOR_INCOMING_QUEUE`                        | Yes       | stubs_executor_incoming_request                                                                                | Stubs executor incoming request queue name                      |
| `EXECUTOR_STUBS_OUTGOING_QUEUE`                        | Yes       | executor_stubs_outgoing_response                                                                               | Executor stubs outgoing response queue name                     |
| `REPORT_QUEUE`                                         | Yes       | ReportExecution                                                                                                | Reports queue name                                              |
| `END_EXCEPTIONAL_SITUATIONS_EVENTS_MESSAGES_TTL`       | No        | 180000                                                                                                         | End exceptional situations events message time-to-live value    |
| `OTHER_TOPICS_MESSAGES_TTL`                            | No        | 180000                                                                                                         | Other topics message time-to-live value                         |
| `EXECUTOR_STUBS_OUTGOING_MESSAGES_TTL`                 | No        | 90000                                                                                                          | Executor stubs outgoing response message time-to-live value     |
| `REPORTS_MESSAGES_TTL`                                 | No        | 1800000                                                                                                        | Reports message time-to-live value                              |
| `REPORT_USE_COMPRESSION`                               | No        | true                                                                                                           | Reports use compression                                         |
| `REPORT_USE_ASYNC_SEND`                                |           | true                                                                                                           | Reports use async send                                          |
| `REPORT_MAX_THREAD_POOL_SIZE`                          | No        | 1200                                                                                                           | Reports max thread pool size value                              |
| `STUBS_EXECUTOR_CONCURRENCY`                           | No        | 120-900                                                                                                        | Stubs executor listener container factory concurrency           |
| `STUBS_EXECUTOR_MAX_MESSAGES_PER_TASK`                 | No        | -1                                                                                                             | Stubs executor listener container factory max messages per task |
| `DATASET_SERVICE_URL`                                  | No        | https://atp-dataset-service-address                                                         | Dataset service url                                             |
| `ENVIRONMENTS_SERVICE_URL`                             | No        | http://atp-environments-service-address                                                     | Environment configurator service url                            |
| `CATALOGUE_URL`                                        | No        | http://atp-catalogue-service-address                                                        | Catalogue url                                                   |
| `ATP_ITF_CONFIGURATOR_URL`                             | No        | http://atp-itf-configurator-service-address                                                 | ITF configurator url                                            |
| `BV_SERVICE_URL`                                       | No        | http://atp-bv-service-address                                                               | Bulk validator service url                                      |
| `EI_SERVICE_URL`                                       | No        | http://atp-ei-service-address                                                               | Export import service url}                                      |
| `HAZELCAST_CACHE_ENABLED`                              | No        | true                                                                                                           | Enable or disable hazelcast cache                               |
| `HAZELCAST_ADDRESS`                                    | No        | 127.0.0.1:5701                                                                                                 | Hazelcast address                                               |
| `HAZELCAST_EXPIRATION_TASK_PERIOD_SECONDS`             | No        | 5                                                                                                              | Hazelcast internal map expiration task period seconds           |
| `HAZELCAST_EXPIRATION_CLEANUP_PERCENTAGE`              | No        | 10                                                                                                             | Hazelcast internal map expiration cleanup percentage            |
| `HAZELCAST_PROJECT_SETTINGS_CACHE_REFILL_TIME_SECONDS` | No        | 3600                                                                                                           | Hazelcast project settings cache refill time seconds            |
| `EI_CLEAN_JOB_WORKDIR`                                 | No        | exportimport/node                                                                                              | Atp export workdir name                                         |
| `EI_CLEAN_JOB_ENABLED`                                 | No        | true                                                                                                           | Enable or disable atp ei file cleanup job                       |
| `EI_CLEAN_SCHEDULED_JOB_PERIOD_MS`                     | No        | 86400000                                                                                                       | Atp ei file cleanup job fixedRate                               |
| `EI_CLEAN_JOB_FILE_DELETE_AFTER_MS`                    | No        | 172800000                                                                                                      | Atp ei file delete after ms                                     |
| `TRANSPORT_CUSTOM_LIB_FOLDER`                          | No        | -                                                                                                              | Transport custom lib folder name                                |
| `EDS_GRIDFS_ENABLED`                                   | No        | true                                                                                                           | Enable or disable external data storage                         |
| `MONGO_DB_ADDR`                                        | No        | localhost                                                                                                      | External data storage database host address                     |
| `MONGO_DB_PORT`                                        | No        | 27017                                                                                                          | External data storage database port number                      |
| `EDS_GRIDFS_DB`                                        | No        | local_itf_gridfs                                                                                               | External data storage database name                             |
| `EDS_GRIDFS_USER`                                      | No        | admin                                                                                                          | External data storage database username value                   |
| `EDS_GRIDFS_PASSWORD`                                  | No        | admin                                                                                                          | External data storage database password value                   |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE`               | No        | 256MB                                                                                                          | Spring servlet multipart max-file-size value                    |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE`            | No        | 256MB                                                                                                          | Spring servlet multipart max-request-size value                 |
| `SCHEDULED_CLEANUP_TEMPFILES_FIXEDRATE_MS`             | No        | 3600000                                                                                                        | Scheduled cleanup tempFiles fixedRate ms                        |
| `SCHEDULED_CLEANUP_TEMPFILES_MODIFIED_BEFORE_MS`       | No        | 3600000                                                                                                        | Scheduled cleanup tempFiles modifiedBefore ms                   |
| `ATP_ITF_EXECUTOR_SSE_TIMEOUT`                         | No        | 60000                                                                                                          | Server-Side Events timeout                                      |
| `MONITOR_PORT`                                         | No        | 8090                                                                                                           | Metric server port number                                       |
| `MONITOR_WEB_EXPOSE`                                   | No        | prometheus,health,info,env                                                                                     | Metric endpoints exposure include                               |
| `MONITOR_WEB_BASE`                                     | No        | /                                                                                                              | Metric endpoints base-path                                      |
| `MONITOR_WEB_MAP_PROM`                                 | No        | metrics                                                                                                        | Metric endpoints path-mapping prometheus                        |
| `MULTI_TENANCY_HIBERNATE_ENABLED`                      | No        | false                                                                                                          | Enable or disable atp multi-tenancy integration                 |
| `SWAGGER_ENABLED`                                      | No        | true                                                                                                           | Enable or disable Swagger integration                           |
| `USERDATA_CLEAN_LEAVE_DAYS`                            | No        | 7                                                                                                              | Userdata clean leave_days                                       |
| `USERDATA_CLEAN_LEAVE_DAYS_TIMEUNIT`                   | No        | days                                                                                                           | Userdata clean leave_days timeunit                              |
| `USERDATA_CLEAN_JOB_EXPRESSION`                        | No        | 0 0 3 1/3 * ?                                                                                                  | CRON userdata clean job expression                              |
| `AUDIT_LOGGING_ENABLE`                                 | No        | false                                                                                                          | Enable or Disable audit logging                                 |
| `AUDIT_LOGGING_TOPIC_NAME`                             | No        | audit_logging_topic                                                                                            | Audit logging Kafka topic name                                  |
| `AUDIT_LOGGING_TOPIC_PARTITIONS`                       | No        | 1                                                                                                              | Audit logging Kafka topic partitions number                     |
| `AUDIT_LOGGING_TOPIC_REPLICAS`                         | No        | 3                                                                                                              | Audit logging Kafka replicas number                             |
| `KAFKA_REPORTING_SERVERS`                              | No        | kafka.reporting.svc:9092                                                                                       | Reporting kafka producer bootstrap server url                   |
| `JAVERS_ENABLED`                                       | No        | false                                                                                                          | Enable or disable javers                                        |
| `JAVERS_HISTORY_ENABLED`                               | No        | true                                                                                                           | Enable or disable javers history                                |
| `HISTORY_CLEAN_JOB_EXPRESSION`                         | No        | 0 10 0 * * ?                                                                                                   | CRON itf history clean job expression                           |
| `HISTORY_CLEAN_JOB_REVISION_MAX_COUNT`                 | No        | 100                                                                                                            | Itf history clean job revision max count                        |
| `HISTORY_CLEAN_JOB_PAGE_SIZE`                          | No        | 100                                                                                                            | Itf history clean job page-size                                 |
| `CONSUL_URL`                                           | No        | localhost                                                                                                      | Consul host number                                              |
| `CONSUL_PORT`                                          | No        | 8500                                                                                                           | Consul port number                                              |
| `CONSUL_ENABLED`                                       | No        | false                                                                                                          | Enable or disable Consul                                        |
| `CONSUL_PREFIX`                                        | No        | devci                                                                                                          | Consul prefix value                                             |
| `CONSUL_TOKEN`                                         | No        | -                                                                                                              | Consul acl_token value                                          |
| `CONSUL_CONFIG_DATA_KEY`                               | No        | data                                                                                                           | Consul config data-key value                                    |
| `CONSUL_CONFIG_FORMAT`                                 | No        | properties                                                                                                     | Consul config format value                                      |

# Helm

## Prerequisites

1. Install k8s locally
2. Install Helm

## How to deploy tool

1. Build snapshot (artifacts and docker image) of https://github.com/Netcracker/qubership-testing-platform-itf-executor in GitHub
2. Clone repository to a place, available from your openshift/kubernetes where you need to deploy the tool to
3. Navigate to <repository-root>/deployments/charts/atp-itf-executor folder
4. Check/change configuration parameters in the ./values.yaml file according to your services installed
5. Execute the command: `helm install atp-itf-executor`
6. After installation is completed, check deployment health
