# Qubership ATP-ITF-EXECUTOR Startup Guide

## How to start backend locally

### Clone repository
   `git clone <atp-itf-executor repository url>`

### Build the project
   `mvn -P github clean install`

### Create or change default configuration [`.run/backend.run.xml`](../.run/backend.run.xml)

   * Go to Run menu and click Edit Configuration
   * Set parameters
   * Add the following parameters in VM options 
     * click "Modify Options" and select "Add VM Options":

**NOTE:** Configuration files [`application.properties`](../common/application.properties) and [`bootstrap.properties`](../common/bootstrap.properties)

**NOTE:** Configuration logging file [`logback-spring.xml`](../common/logback-spring.xml)

```properties
##==============================DataBase Settings======================
spring.datasource.url=${JDBC_URL}
spring.datasource.username=${ITF_EXECUTOR_DB_USER}
spring.datasource.password=${ITF_EXECUTOR_DB_PASSWORD}
spring.datasource.hikari.minimum-idle=${SPRING_DATASOURCE_MINIDLE}
spring.datasource.hikari.maximum-pool-size=${SPRING_DATASOURCE_MAXTOTAL}
spring.datasource.hikari.idle-timeout=${SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT}
spring.datasource.hikari.max-lifetime=${SPRING_DATASOURCE_HIKARI_MAX_LIFETIME}
##==============================Undertow===============================
server.port=${HTTP_PORT}
embedded.https.enabled=${EMBEDDED_HTTPS_ENABLED}
embedded.tls.server.port=${EMBEDDED_TLS_SERVER_PORT}
embedded.ssl.server.port=${EMBEDDED_SSL_SERVER_PORT}
keystore.password=${KEYSTORE_PASSWORD}
# Undertow session timeout (in minutes) before authorization is expired
server.servlet.session.timeout=${UNDERTOW_SESSION_TIMEOUT}
server.compression.enabled=${UNDERTOW_COMPRESSION_ENABLED}
server.compression.mime-types=${UNDERTOW_COMPRESSION_MIMETYPE}
server.undertow.threads.io=${SERVER_UNDERTOW_IO_THREADS}
server.undertow.threads.worker=${SERVER_UNDERTOW_WORKER_THREADS}
server.undertow.accesslog.enabled=${SERVER_UNDERTOW_ACCESSLOG_ENABLED}
jboss.threads.eqe.statistics=${JBOSS_THREADS_EQE_STATISTICS}
##===============================ATP ITF===============================
#Parameter for Java GC to avoid the uncontrolled memory consumption
max.ram.size=${MAX_RAM_SIZE}
jpda.enabled=${JPDA_ENABLED}
##===============================Logging===============================
logging.level.root=${LOG_LEVEL}
log.graylog.on=${GRAYLOG_ON}
log.graylog.host=${GRAYLOG_HOST}
log.graylog.port=${GRAYLOG_PORT}
##==================Integration with Spring Cloud======================
spring.application.name=${SERVICE_NAME}
eureka.client.enabled=${EUREKA_CLIENT_ENABLED}
eureka.client.serviceUrl.defaultZone=${SERVICE_REGISTRY_URL}
##==================Integration with ATP======================
atp.service.public=${ATP_SERVICE_PUBLIC}
atp.service.internal=${ATP_SERVICE_INTERNAL}
atp.service.path=${ATP_SERVICE_PATH}
##==================atp-auth-spring-boot-starter=======================
spring.profiles.active=${SPRING_PROFILES}
keycloak.enabled=${KEYCLOAK_ENABLED}
keycloak.resource=${KEYCLOAK_CLIENT_NAME}
keycloak.credentials.secret=${KEYCLOAK_SECRET}
keycloak.realm=${KEYCLOAK_REALM}
keycloak.auth-server-url=${KEYCLOAK_AUTH_URL}
atp-auth.project_info_endpoint=${PROJECT_INFO_ENDPOINT}
atp-auth.headers.content-security-policy=${CONTENT_SECURITY_POLICY}
##=============================Feign===================================
atp.public.gateway.url=${ATP_PUBLIC_GATEWAY_URL}
atp.internal.gateway.url=${ATP_INTERNAL_GATEWAY_URL}
atp.internal.gateway.enabled=${ATP_INTERNAL_GATEWAY_ENABLED}
atp.internal.gateway.name=${ATP_INTERNAL_GATEWAY_NAME}
feign.httpclient.disableSslValidation=${FEIGN_HTTPCLIENT_DISABLE_SSL}
feign.httpclient.enabled=${FEIGN_HTTPCLIENT_ENABLED}
feign.okhttp.enabled=${FEIGN_OKHTTP_ENABLED}
## datasets
feign.atp.datasets.name=${FEIGN_ATP_DATASETS_NAME}
feign.atp.datasets.url=${FEIGN_ATP_DATASETS_URL}
feign.atp.datasets.route=${FEIGN_ATP_DATASETS_ROUTE}
## bulk validator
feign.atp.bv.name=${FEIGN_ATP_BV_NAME}
feign.atp.bv.url=${FEIGN_ATP_BV_URL}
feign.atp.bv.route=${FEIGN_ATP_BV_ROUTE}
## environments
feign.atp.environments.name=${FEIGN_ATP_ENVIRONMENTS_NAME}
feign.atp.environments.url=${FEIGN_ATP_ENVIRONMENTS_URL}
feign.atp.environments.route=${FEIGN_ATP_ENVIRONMENTS_ROUTE}
## catalogue
feign.atp.catalogue.name=${FEIGN_ATP_CATALOGUE_NAME}
feign.atp.catalogue.route=${FEIGN_ATP_CATALOGUE_ROUTE}
feign.atp.catalogue.url=${FEIGN_ATP_CATALOGUE_URL}
## users
feign.atp.users.url=${FEIGN_ATP_USERS_URL}
feign.atp.users.name=${FEIGN_ATP_USERS_NAME}
feign.atp.users.route=${FEIGN_ATP_USERS_ROUTE}
## itf executor
feign.atp.executor.name=${FEIGN_ATP_ITF_EXECUTOR_NAME}
feign.atp.executor.url=${FEIGN_ATP_ITF_EXECUTOR_URL}
feign.atp.executor.route=${FEIGN_ATP_ITF_EXECUTOR_ROUTE}
## itf reports
feign.atp.reports.name=${FEIGN_ATP_ITF_REPORTS_NAME}
feign.atp.reports.url=${FEIGN_ATP_ITF_REPORTS_URL}
feign.atp.reports.route=${FEIGN_ATP_ITF_REPORTS_ROUTE}
## ei
feign.atp.ei.name=${FEIGN_ATP_ITF_EI_NAME}
feign.atp.ei.url=${FEIGN_ATP_ITF_EI_URL}
feign.atp.ei.route=${FEIGN_ATP_ITF_EI_ROUTE}
##========================Feign timeout================================
feign.client.config.default.connectTimeout=${FEIGN_CONNECT_TIMEOUT}
feign.client.config.default.readTimeout=${FEIGN_READ_TIMEOUT}
##==================GridFS==============================================
ei.gridfs.database=${EI_GRIDFS_DB}
ei.gridfs.host=${EI_GRIDFS_DB_ADDR}
ei.gridfs.port=${EI_GRIDFS_DB_PORT}
ei.gridfs.user=${EI_GRIDFS_USER}
ei.gridfs.password=${EI_GRIDFS_PASSWORD}
##=============================Kafka===================================
kafka.enable=${KAFKA_ENABLE}
kafka.topic=${KAFKA_TOPIC}
kafka.topic.itf.lite.export.start=${KAFKA_ITF_LITE_EXPORT_ITF_START_TOPIC}
kafka.topic.itf.lite.export.finish=${KAFKA_ITF_LITE_EXPORT_ITF_FINISH_TOPIC}
kafka.topic.environments_notification=${KAFKA_ENVIRONMENTS_NOTIFICATION_TOPIC}
kafka.service.entities.topic=${KAFKA_SERVICE_ENTITIES_TOPIC}
kafka.service.entities.topic.partitions=${KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS}
kafka.service.entities.topic.replicas=${KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR}
service.entities.migration.enabled=${SERVICE_ENTITIES_MIGRATION_ENABLED}
spring.kafka.producer.bootstrap-servers=${KAFKA_SERVERS}
spring.kafka.consumer.bootstrap-servers=${KAFKA_SERVERS}
spring.kafka.consumer.client-id=${KAFKA_CLIENT_ID}
spring.kafka.consumer.group-id=${KAFKA_GROUP_ID}
##==================Zipkin=====================
spring.sleuth.enabled=${ZIPKIN_ENABLE}
spring.sleuth.sampler.probability=${ZIPKIN_PROBABILITY}
spring.zipkin.baseUrl=${ZIPKIN_URL}
##=============================ActiveMQ===============================
message-broker.url=${ATP_ITF_BROKER_URL_TCP}
message-broker.url.ws=${ATP_ITF_BROKER_URL_WS}
message-broker.configurator-executor-event-triggers.topic=${CONFIGURATOR_EXECUTOR_EVENT_TRIGGERS_TOPIC}
message-broker.executor-configurator-event-triggers.topic=${EXECUTOR_CONFIGURATOR_EVENT_TRIGGERS_TOPIC}
message-broker.executor-stubs-sync.topic=${EXECUTOR_STUBS_SYNC_TOPIC}
message-broker.configurator-stubs.topic=${CONFIGURATOR_STUBS_TOPIC}
message-broker.stubs-configurator.topic=${STUBS_CONFIGURATOR_TOPIC}
message-broker.end-exceptional-situations-events.topic=${END_EXCEPTIONAL_SITUATIONS_EVENTS_TOPIC}
message-broker.executor-tccontext-operations.topic=${EXECUTOR_TCCONTEXT_OPERATIONS_TOPIC}
message-broker.executor-disabling-stepbystep.topic=${EXECUTOR_DISABLING_STEPBYSTEP_TOPIC}
message-broker.executor-sync-reload-dictionary.topic=${EXECUTOR_SYNC_RELOAD_DICTIONARY_TOPIC}
message-broker.eds-update.topic=${EDS_UPDATE_TOPIC}
message-broker.stubs-executor-incoming-request.queue=${STUBS_EXECUTOR_INCOMING_QUEUE}
message-broker.executor-stubs-outgoing-response.queue=${EXECUTOR_STUBS_OUTGOING_QUEUE}
message-broker.reports.queue=${REPORT_QUEUE}
message-broker.end-exceptional-situations-events.message-time-to-live=${END_EXCEPTIONAL_SITUATIONS_EVENTS_MESSAGES_TTL}
message-broker.other-topics.message-time-to-live=${OTHER_TOPICS_MESSAGES_TTL}
message-broker.executor-stubs-outgoing-response.message-time-to-live=${EXECUTOR_STUBS_OUTGOING_MESSAGES_TTL}
message-broker.reports.message-time-to-live=${REPORTS_MESSAGES_TTL}
message-broker.reports.useCompression=${REPORT_USE_COMPRESSION}
message-broker.reports.useAsyncSend=${REPORT_USE_ASYNC_SEND}
message-broker.reports.maxThreadPoolSize=${REPORT_MAX_THREAD_POOL_SIZE}
message-broker.stubs-executor.listenerContainerFactory.concurrency=${STUBS_EXECUTOR_CONCURRENCY}
message-broker.stubs-executor.listenerContainerFactory.maxMessagesPerTask=${STUBS_EXECUTOR_MAX_MESSAGES_PER_TASK}
##======================ATP Services integration=======================
dataset.service.url=${DATASET_SERVICE_URL}
environments.service.url=${ENVIRONMENTS_SERVICE_URL}
atp.catalogue.url=${CATALOGUE_URL}
configurator.url=${ATP_ITF_CONFIGURATOR_URL}
bv.service.url=${BV_SERVICE_URL}
ei.service.url=${EI_SERVICE_URL}
##======================Hazelcast configurations=======================
hazelcast.cache.enabled=${HAZELCAST_CACHE_ENABLED}
hazelcast.address=${HAZELCAST_ADDRESS}
hazelcast.internal.map.expiration.task.period.seconds=${HAZELCAST_EXPIRATION_TASK_PERIOD_SECONDS}
hazelcast.internal.map.expiration.cleanup.percentage=${HAZELCAST_EXPIRATION_CLEANUP_PERCENTAGE}
hazelcast.project-settings.cache.refill.time.seconds=${HAZELCAST_PROJECT_SETTINGS_CACHE_REFILL_TIME_SECONDS}
##=============================Export/Import=============================
atp.export.workdir=${EI_CLEAN_JOB_WORKDIR}
atp.ei.file.cleanup.job.enable=${EI_CLEAN_JOB_ENABLED}
atp.ei.file.cleanup.job.fixedRate=${EI_CLEAN_SCHEDULED_JOB_PERIOD_MS}
atp.ei.file.delete.after.ms=${EI_CLEAN_JOB_FILE_DELETE_AFTER_MS}
##=============================Transports=============================
#Custom transport lib
transport.lib=${TRANSPORT_CUSTOM_LIB_FOLDER}
##=============================File Uploader Settings=============================
eds.gridfs.enabled=${EDS_GRIDFS_ENABLED}
eds.gridfs.host=${MONGO_DB_ADDR}
eds.gridfs.port=${MONGO_DB_PORT}
eds.gridfs.database=${EDS_GRIDFS_DB}
eds.gridfs.username=${EDS_GRIDFS_USER}
eds.gridfs.password=${EDS_GRIDFS_PASSWORD}
spring.servlet.multipart.max-file-size=${SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE}
spring.servlet.multipart.max-request-size=${SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE}
scheduled.cleanup.tempFiles.fixedRate.ms=${SCHEDULED_CLEANUP_TEMPFILES_FIXEDRATE_MS}
scheduled.cleanup.tempFiles.modifiedBefore.ms=${SCHEDULED_CLEANUP_TEMPFILES_MODIFIED_BEFORE_MS}
##=============================Server-Side Events settings=============================
atp-itf-executor.sse-timeout=${ATP_ITF_EXECUTOR_SSE_TIMEOUT}
##=================Monitoring==========================================
management.server.port=${MONITOR_PORT}
management.endpoints.web.exposure.include=${MONITOR_WEB_EXPOSE}
management.endpoints.web.base-path=${MONITOR_WEB_BASE}
management.endpoints.web.path-mapping.prometheus=${MONITOR_WEB_MAP_PROM}
##===============Hibernate-multi-tenancy=================================
atp.multi-tenancy.enabled=${MULTI_TENANCY_HIBERNATE_ENABLED}
##=================== Swagger =======================
springdoc.api-docs.enabled=${SWAGGER_ENABLED}
##==================Scheduler======================
userdata.clean.leave_days=${USERDATA_CLEAN_LEAVE_DAYS}
userdata.clean.leave_days.timeunit=${USERDATA_CLEAN_LEAVE_DAYS_TIMEUNIT}
userdata.clean.job.expression=${USERDATA_CLEAN_JOB_EXPRESSION}
##=============Audit Logging=================
atp.audit.logging.enable=${AUDIT_LOGGING_ENABLE}
atp.audit.logging.topic.name=${AUDIT_LOGGING_TOPIC_NAME}
atp.audit.logging.topic.partitions=${AUDIT_LOGGING_TOPIC_PARTITIONS}
atp.audit.logging.topic.replicas=${AUDIT_LOGGING_TOPIC_REPLICAS}
atp.reporting.kafka.producer.bootstrap-server=${KAFKA_REPORTING_SERVERS}
##==================Javers for ITF History=====================
javers.auditableAspectEnabled=${JAVERS_ENABLED}
javers.springDataAuditableRepositoryAspectEnabled=${JAVERS_ENABLED}
##=============================History Settings=============================
javers.history.enabled=${JAVERS_HISTORY_ENABLED}
##==================Scheduler======================
atp.itf.history.clean.job.expression=${HISTORY_CLEAN_JOB_EXPRESSION}
atp.itf.history.clean.job.revision.max.count=${HISTORY_CLEAN_JOB_REVISION_MAX_COUNT}
atp.itf.history.clean.job.page-size=${HISTORY_CLEAN_JOB_PAGE_SIZE}
##=============================Consul==================================
spring.cloud.consul.host=${CONSUL_URL}
spring.cloud.consul.port=${CONSUL_PORT}
spring.cloud.consul.enabled=${CONSUL_ENABLED}
spring.cloud.consul.config.enabled=${CONSUL_ENABLED}
spring.cloud.consul.config.prefix=${CONSUL_PREFIX}
spring.cloud.consul.config.acl_token=${CONSUL_TOKEN}
spring.cloud.consul.config.data-key=${CONSUL_CONFIG_DATA_KEY}
spring.cloud.consul.config.format=${CONSUL_CONFIG_FORMAT}
##=====================================================================
```

5. Click `Apply` and `OK`

6. Run the project
