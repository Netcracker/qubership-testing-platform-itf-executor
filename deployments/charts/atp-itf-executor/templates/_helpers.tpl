{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
{{ printf "- name: %s" $key }}
{{ printf "  value: \"%s\"" $val }}
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- range $keys }}
{{ printf "- name: %s" . }}
{{ printf "  valueFrom:" }}
{{ printf "    secretKeyRef:" }}
{{ printf "      name: %s-secrets" $.Values.SERVICE_NAME }}
{{ printf "      key: %s" . }}
{{- end }}
{{- end }}

{{- define "env.host" -}}
{{- $url := .Values.ATP_ITF_EXECUTOR_URL -}}
{{- if $url -}}
{{- regexReplaceAll "http(s)?://(.*)" $url "${2}" -}}
{{- else -}}
{{- $hosts := dict "KUBERNETES" "dev-kubernetes-address" "OPENSHIFT" "dev-cloud-address" -}}
{{- print .Values.SERVICE_NAME "-" .Release.Namespace "." (.Values.CLOUD_PUBLIC_HOST | default (index $hosts .Values.PAAS_PLATFORM)) -}}
{{- end -}}
{{- end -}}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ACTIVEMQ_PORT: "{{ .Values.ACTIVEMQ_PORT_PARAM }}"
ATP_INTEGRATION_ENABLED: "{{ .Values.ATP_INTEGRATION_ENABLED }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.ATP_INTERNAL_GATEWAY_NAME }}"
ATP_INTERNAL_GATEWAY_URL: "{{ .Values.ATP_INTERNAL_GATEWAY_URL }}"
ATP_ITF_BROKER_URL_TCP: "{{ .Values.ATP_ITF_BROKER_URL_TCP }}"
ATP_ITF_BROKER_URL_WS: "{{ .Values.ATP_ITF_BROKER_URL_WS }}"
ATP_ITF_CONFIGURATOR_URL: "{{ .Values.ATP_ITF_CONFIGURATOR_URL }}"
ATP_ITF_EXECUTOR_SSE_TIMEOUT: "{{ .Values.ATP_ITF_EXECUTOR_SSE_TIMEOUT }}"
ATP_ITF_STUBS_URL: "{{ .Values.ATP_ITF_STUBS_URL }}"
ATP_LOGGER_URL: "{{ .Values.ATP_LOGGER_URL }}"
ATP_PUBLIC_GATEWAY_URL: "{{ .Values.ATP_PUBLIC_GATEWAY_URL }}"
ATP_RAM_RECEIVER_URL: "{{ .Values.ATP_RAM_RECEIVER_URL }}"
ATP_RAM_URL: "{{ .Values.ATP_RAM_URL }}"
ATP_SERVICE_INTERNAL: "{{ .Values.ATP_SERVICE_INTERNAL }}"
ATP_SERVICE_PUBLIC: "{{ .Values.ATP_SERVICE_PUBLIC }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: "{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}"
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
BV_SERVICE_URL: "{{ .Values.ATP_BVT_URL }}"
CATALOGUE_URL: "{{ .Values.CATALOGUE_URL }}"
CONFIGURATOR_EXECUTOR_EVENT_TRIGGERS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.CONFIGURATOR_EXECUTOR_EVENT_TRIGGERS_TOPIC "def" "configurator_executor_event_triggers") }}"
CONFIGURATOR_STUBS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.CONFIGURATOR_STUBS_TOPIC "def" "configurator_stubs") }}"
CONSUL_CONFIG_DATA_KEY: "{{ .Values.CONSUL_CONFIG_DATA_KEY }}"
CONSUL_CONFIG_FORMAT: "{{ .Values.CONSUL_CONFIG_FORMAT }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_HEALTH_CHECK_ENABLED: "{{ .Values.CONSUL_HEALTH_CHECK_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
DATASET_SERVICE_URL: "{{ .Values.ATP_DATASET_URL }}"
EDS_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EDS_GRIDFS_DB "def" "itf_eds_gridfs") }}"
EDS_GRIDFS_ENABLED: "{{ .Values.EDS_GRIDFS_ENABLED }}"
EDS_UPDATE_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EDS_UPDATE_TOPIC "def" "eds_update") }}"
EI_GRIDFS_ENABLED: "{{ .Values.EI_GRIDFS_ENABLED }}"
EI_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_DB "def" "atp-ei-gridfs") }}"
EI_GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
EI_GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
EI_SERVICE_URL: "{{ .Values.ATP_EXPORT_IMPORT_URL }}"
EMBEDDED_HTTPS_ENABLED: "{{ .Values.EMBEDDED_HTTPS_ENABLED }}"
END_EXCEPTIONAL_SITUATIONS_EVENTS_MESSAGES_TTL: "{{ .Values.END_EXCEPTIONAL_SITUATIONS_EVENTS_MESSAGES_TTL }}"
END_EXCEPTIONAL_SITUATIONS_EVENTS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.END_EXCEPTIONAL_SITUATIONS_EVENTS_TOPIC "def" "end_exceptional_situations_events") }}"
ENVIRONMENTS_SERVICE_URL: "{{ .Values.ATP_ENVIRONMENTS_URL }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
EXECUTOR_CONFIGURATOR_EVENT_TRIGGERS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_CONFIGURATOR_EVENT_TRIGGERS_TOPIC "def" "executor_configurator_event_triggers") }}"
EXECUTOR_RESUMING_MESSAGE_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_RESUMING_MESSAGE_TOPIC "def" "executor_resuming_message") }}"
EXECUTOR_STUBS_OUTGOING_MESSAGES_TTL: "{{ .Values.EXECUTOR_STUBS_OUTGOING_MESSAGES_TTL }}"
EXECUTOR_STUBS_OUTGOING_QUEUE: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_STUBS_OUTGOING_QUEUE "def" "executor_stubs_outgoing_response") }}"
EXECUTOR_STUBS_SYNC_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_STUBS_SYNC_TOPIC "def" "executor_stubs_sync") }}"
EXECUTOR_SYNC_RELOAD_DICTIONARY_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_SYNC_RELOAD_DICTIONARY_TOPIC "def" "executor_sync_reload_dictionary") }}"
EXECUTOR_TCCONTEXT_OPERATIONS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_TCCONTEXT_OPERATIONS_TOPIC "def" "executor_tccontext_operations") }}"
EXECUTOR_DISABLING_STEPBYSTEP_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.EXECUTOR_DISABLING_STEPBYSTEP_TOPIC "def" "executor_disabling_stepbystep") }}"
FEIGN_ATP_BV_NAME: "ATP-BV"
FEIGN_ATP_BV_ROUTE: "api/bvtool/v1"
FEIGN_ATP_BV_URL: ""
FEIGN_ATP_CATALOGUE_NAME: "{{ .Values.FEIGN_ATP_CATALOGUE_NAME }}"
FEIGN_ATP_CATALOGUE_ROUTE: "{{ .Values.FEIGN_ATP_CATALOGUE_ROUTE }}"
FEIGN_ATP_CATALOGUE_URL: "{{ .Values.FEIGN_ATP_CATALOGUE_URL }}"
FEIGN_ATP_DATASETS_NAME: "ATP-DATASETS"
FEIGN_ATP_DATASETS_ROUTE: "api/atp-datasets/v1"
FEIGN_ATP_DATASETS_URL: ""
FEIGN_ATP_EI_NAME: "{{ .Values.FEIGN_ATP_EI_NAME }}"
FEIGN_ATP_EI_ROUTE: "{{ .Values.FEIGN_ATP_EI_ROUTE }}"
FEIGN_ATP_EI_URL: "{{ .Values.FEIGN_ATP_EI_URL }}"
FEIGN_ATP_ENVIRONMENTS_NAME: "ATP-ENVIRONMENTS"
FEIGN_ATP_ENVIRONMENTS_ROUTE: "api/atp-environments/v1"
FEIGN_ATP_ENVIRONMENTS_URL: ""
FEIGN_ATP_ITF_EXECUTOR_URL: "{{ .Values.FEIGN_ATP_ITF_EXECUTOR_URL }}"
FEIGN_ATP_ITF_REPORTS_NAME: "{{ .Values.FEIGN_ATP_ITF_REPORTS_NAME }}"
FEIGN_ATP_ITF_REPORTS_ROUTE: "{{ .Values.FEIGN_ATP_ITF_REPORTS_ROUTE }}"
FEIGN_ATP_ITF_REPORTS_URL: "{{ .Values.FEIGN_ATP_ITF_REPORTS_URL }}"
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
FEIGN_HTTPCLIENT_DISABLE_SSL: "true"
FEIGN_HTTPCLIENT_ENABLED: "false"
FEIGN_OKHTTP_ENABLED: "true"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
HAZELCAST_ADDRESS: "{{ .Values.HAZELCAST_ADDRESS }}"
HAZELCAST_CACHE_ENABLED: "{{ .Values.HAZELCAST_CACHE_ENABLED }}"
HAZELCAST_EXPIRATION_CLEANUP_PERCENTAGE: "{{ .Values.HAZELCAST_EXPIRATION_CLEANUP_PERCENTAGE }}"
HAZELCAST_EXPIRATION_TASK_PERIOD_SECONDS: "{{ .Values.HAZELCAST_EXPIRATION_TASK_PERIOD_SECONDS }}"
HAZELCAST_PROJECT_SETTINGS_CACHE_REFILL_TIME_SECONDS: "{{ .Values.HAZELCAST_PROJECT_SETTINGS_CACHE_REFILL_TIME_SECONDS}}"
JAVA_OPTIONS: "{{ if .Values.HEAPDUMP_ENABLED }}-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/diagnostic{{ end }} -Dcom.sun.management.jmxremote={{ .Values.JMX_ENABLE }} -Dcom.sun.management.jmxremote.port={{ .Values.JMX_PORT }} -Dcom.sun.management.jmxremote.rmi.port={{ .Values.JMX_RMI_PORT }} -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dexecutor.thread.pool.size={{ .Values.EXECUTOR_THREAD_POOL_SIZE }} -Dexecutor.thread.pool.core.size={{ .Values.EXECUTOR_THREAD_POOL_CORE_SIZE }} -Dbackground.executor.thread.pool.size={{ .Values.BACKGROUND_EXECUTOR_THREAD_POOL_SIZE }} -Dspring.datasource.hikari.maximum-pool-size={{ .Values.SPRING_DATASOURCE_MAXTOTAL }} -Dspring.datasource.hikari.minimum-idle={{ .Values.SPRING_DATASOURCE_MINIDLE }} -Dserver.undertow.threads.io={{ .Values.SERVER_UNDERTOW_IO_THREADS }} -Dserver.undertow.threads.worker={{ .Values.SERVER_UNDERTOW_WORKER_THREADS }} -Djboss.threads.eqe.statistics={{ .Values.JBOSS_THREADS_EQE_STATISTICS }} -XX:NewRatio={{ .Values.NEWRATIO }}"
JDBC_URL: "jdbc:postgresql://{{ .Values.PG_DB_ADDR }}:{{ .Values.PG_DB_PORT }}/{{ include "env.default" (dict "ctx" . "val" .Values.ITF_EXECUTOR_DB "def" .Values.SERVICE_NAME ) }}"
KAFKA_CLIENT_ID: "atp-itf-executor-{{ .Release.Namespace }}"
KAFKA_ENABLE: "{{ .Values.KAFKA_ENABLE }}"
KAFKA_ENVIRONMENTS_NOTIFICATION_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ENVIRONMENTS_NOTIFICATION_TOPIC "def" "environments_notification_topic") }}"
KAFKA_GROUP_ID: "atp-itf-executor-{{ .Release.Namespace }}"
KAFKA_ITF_LITE_EXPORT_ITF_FINISH_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXPORT_ITF_FINISH_TOPIC "def" "itf_lite_to_itf_export_finish") }}"
KAFKA_ITF_LITE_EXPORT_ITF_START_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXPORT_ITF_START_TOPIC "def" "itf_lite_to_itf_export_start") }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC "def" "orch_logrecord_context_topic") }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC "def" "orch_logrecord_message_parameters_topic") }}"
KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC "def" "orch_logrecord_step_context_topic") }}"
KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_LOGRECORD_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_TOPIC "def" "orch_logrecord_topic") }}"
KAFKA_LOGRECORD_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_REPORTING_SERVERS: "{{ .Values.KAFKA_REPORTING_SERVERS }}"
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_SERVICE_ENTITIES_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SERVICE_ENTITIES_TOPIC "def" "service_entities") }}"
KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS: "{{ .Values.KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS }}"
KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_TOPIC "def" "catalog_notification_topic") }}"
KEYCLOAK_AUTH_SERVER_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_DISABLE_SSL: "true"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "atp2"
KEYSTORE_FILE: "{{ .Values.KEYSTORE_FILE }}"
MAX_RAM_SIZE: "{{ .Values.MAX_RAM_SIZE }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
MONGO_DB_ADDR: "{{ .Values.MONGO_DB_ADDR }}"
MONGO_DB_PORT: "{{ .Values.MONGO_DB_PORT }}"
MULTI_TENANCY_HIBERNATE_ENABLED: "{{ .Values.MULTI_TENANCY_HIBERNATE_ENABLED }}"
OTHER_TOPICS_MESSAGES_TTL: "{{ .Values.OTHER_TOPICS_MESSAGES_TTL }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
REPORTS_MESSAGES_TTL: "{{ .Values.REPORTS_MESSAGES_TTL }}"
REPORT_MAX_THREAD_POOL_SIZE: "{{ .Values.REPORT_MAX_THREAD_POOL_SIZE }}"
REPORT_QUEUE: "{{ include "env.default" (dict "ctx" . "val" .Values.REPORT_QUEUE "def" "ReportExecution") }}"
REPORT_USE_ASYNC_SEND: "{{ .Values.REPORT_USE_ASYNC_SEND }}"
REPORT_USE_COMPRESSION: "{{ .Values.REPORT_USE_COMPRESSION }}"
RUNNING_URL: "{{ .Values.ATP_ITF_EXECUTOR_URL }}"
SCHEDULED_CLEANUP_TEMPFILES_FIXEDRATE_MS: "{{ .Values.SCHEDULED_CLEANUP_TEMPFILES_FIXEDRATE_MS }}"
SCHEDULED_CLEANUP_TEMPFILES_MODIFIED_BEFORE_MS: "{{ .Values.SCHEDULED_CLEANUP_TEMPFILES_MODIFIED_BEFORE_MS }}"
SERVER_UNDERTOW_ACCESSLOG_ENABLED: "{{ .Values.SERVER_UNDERTOW_ACCESSLOG_ENABLED }}"
SERVICE_ENTITIES_MIGRATION_ENABLED: "{{ .Values.SERVICE_ENTITIES_MIGRATION_ENABLED }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: "{{ .Values.SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE }}"
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE: "{{ .Values.SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE }}"
STUBS_CONFIGURATOR_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.STUBS_CONFIGURATOR_TOPIC "def" "stubs_configurator") }}"
STUBS_EXECUTOR_CONCURRENCY: "{{ .Values.STUBS_EXECUTOR_CONCURRENCY }}"
STUBS_EXECUTOR_INCOMING_QUEUE: "{{ include "env.default" (dict "ctx" . "val" .Values.STUBS_EXECUTOR_INCOMING_QUEUE "def" "stubs_executor_incoming_request") }}"
STUBS_EXECUTOR_MAX_MESSAGES_PER_TASK: "{{ .Values.STUBS_EXECUTOR_MAX_MESSAGES_PER_TASK }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
TRANSPORT_CUSTOM_LIB_FOLDER: "{{ .Values.TRANSPORT_CUSTOM_LIB_FOLDER }}"
UNDERTOW_COMPRESSION_ENABLED: "{{ .Values.UNDERTOW_COMPRESSION_ENABLED }}"
UNDERTOW_COMPRESSION_MIMETYPE: "{{ .Values.UNDERTOW_COMPRESSION_MIMETYPE }}"
UNDERTOW_SESSION_TIMEOUT: "{{ .Values.UNDERTOW_SESSION_TIMEOUT }}"
USERDATA_CLEAN_JOB_EXPRESSION: "{{ .Values.USERDATA_CLEAN_JOB_EXPRESSION }}"
USERDATA_CLEAN_LEAVE_DAYS: "{{ .Values.USERDATA_CLEAN_LEAVE_DAYS }}"
USERDATA_CLEAN_LEAVE_DAYS_TIMEUNIT: "{{ .Values.USERDATA_CLEAN_LEAVE_DAYS_TIMEUNIT }}"
EI_CLEAN_JOB_WORKDIR: "{{ .Values.EI_CLEAN_JOB_WORKDIR }}"
EI_CLEAN_JOB_ENABLED: "{{ .Values.EI_CLEAN_JOB_ENABLED }}"
EI_CLEAN_SCHEDULED_JOB_PERIOD_MS: "{{ .Values.EI_CLEAN_SCHEDULED_JOB_PERIOD_MS }}"
EI_CLEAN_JOB_FILE_DELETE_AFTER_MS: "{{ .Values.EI_CLEAN_JOB_FILE_DELETE_AFTER_MS }}"
ZIPKIN_ENABLE: "{{ .Values.ZIPKIN_ENABLE }}"
ZIPKIN_PROBABILITY: "{{ .Values.ZIPKIN_PROBABILITY }}"
ZIPKIN_URL: "{{ .Values.ZIPKIN_URL }}"
EXCLUDE_REGISTRY_METRICS_TAGS: "{{ .Values.EXCLUDE_REGISTRY_METRICS_TAGS }}"
{{- end }}

{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
ITF_EXECUTOR_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.ITF_EXECUTOR_DB_USER "def" .Values.SERVICE_NAME ) }}"
ITF_EXECUTOR_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.ITF_EXECUTOR_DB_PASSWORD "def" .Values.SERVICE_NAME ) }}"
KEYCLOAK_CLIENT_NAME: "{{ default "atp-itf" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "71b6a213-e3b0-4bf4-86c8-dfe11ce9e248" .Values.KEYCLOAK_SECRET }}"
KEYSTORE_PASSWORD: "{{ .Values.KEYSTORE_PASSWORD }}"
EI_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_PASSWORD "def" "atp-ei-gridfs") }}"
EI_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_USER "def" "atp-ei-gridfs") }}"
EDS_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EDS_GRIDFS_PASSWORD "def" "atp-itf-eds-gridfs") }}"
EDS_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EDS_GRIDFS_USER "def" "atp-itf-eds-gridfs") }}"
{{- if .Values.MULTI_TENANCY_HIBERNATE_ENABLED -}}
{{- $additionalClusters := .Values.additionalClusters -}}
{{- if $additionalClusters -}}
{{- range $cluster, $params := .Values.additionalClusters }}
ADDITIONAL_PG_{{ print $cluster "_URL" | upper }}: "{{ print $params.url }}"
ADDITIONAL_PG_{{ print $cluster "_USERNAME" | upper }}: "{{ print $params.user }}"
ADDITIONAL_PG_{{ print $cluster "_PASSWORD" | upper }}: "{{ print $params.password }}"
ADDITIONAL_PG_{{ print $cluster "_DRIVER" | upper }}: "{{ print $params.driver }}"
ADDITIONAL_PG_{{ print $cluster "_PROJECTS" | upper }}: "{{ print ($params.projects | join ",") }}"
{{- end }}
{{- end }}
{{- end }}
{{- end }}

{{- define "env.deploy" }}
ei_gridfs_pass: "{{ .Values.ei_gridfs_pass }}"
ei_gridfs_user: "{{ .Values.ei_gridfs_user }}"
mongo_pass: "{{ .Values.mongo_pass }}"
mongo_user: "{{ .Values.mongo_user }}"
pg_pass: "{{ .Values.pg_pass }}"
pg_user: "{{ .Values.pg_user }}"
ITF_EXECUTOR_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.ITF_EXECUTOR_DB "def" .Values.SERVICE_NAME ) }}"
PG_DB_ADDR: "{{ .Values.PG_DB_ADDR }}"
PG_DB_PORT: "{{ .Values.PG_DB_PORT }}"
{{- end }}