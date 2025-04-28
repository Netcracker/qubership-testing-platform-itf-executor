#!/bin/sh

if [ "${ATP_INTERNAL_GATEWAY_ENABLED}" = "true" ]; then
  echo "Internal gateway integration is enabled."
  FEIGN_ATP_CATALOGUE_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_USERS_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_ITF_REPORTS_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_DATASETS_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_BV_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_ENVIRONMENTS_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_ITF_EXECUTOR_NAME=$ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_EI_NAME=$ATP_INTERNAL_GATEWAY_NAME
else
  echo "Internal gateway integration is disabled."
  FEIGN_ATP_USERS_ROUTE=
  FEIGN_ATP_ITF_REPORTS_ROUTE=
  FEIGN_ATP_DATASETS_ROUTE=
  FEIGN_ATP_BV_ROUTE=
  FEIGN_ATP_ENVIRONMENTS_ROUTE=
  FEIGN_ATP_ITF_STUBS_ROUTE=
  FEIGN_ATP_EI_ROUTE=
fi

# *** Set JVM options
JAVA_OPTIONS="${JAVA_OPTIONS} -Dsession.process.timeout=35000"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dhttp.socket.timeout=300000"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dhttp.connection.timeout=10000"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dhttp.connection-manager.timeout=10000"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.config.location=application.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.cloud.bootstrap.location=bootstrap.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlogging.config=logback-spring.xml"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.on=${GRAYLOG_ON}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.host=${GRAYLOG_HOST}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.port=${GRAYLOG_PORT}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Datp.ram.url=${ATP_RAM_URL}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Datp.logger.url=${ATP_LOGGER_URL}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dhostname=$(hostname)"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dbootstrap.servers=${KAFKA_SERVERS:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.topic.name=${KAFKA_LOGRECORD_TOPIC:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.topic.partitions.number=${KAFKA_LOGRECORD_TOPIC_PARTITIONS:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.topic.replication.factor=${KAFKA_LOGRECORD_TOPIC_REPLICATION_FACTOR:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.context.topic.name=${KAFKA_LOGRECORD_CONTEXT_TOPIC:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.context.topic.partitions.number=${KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.context.topic.replication.factor=${KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.step.context.topic.name=${KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.step.context.topic.partitions.number=${KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_PARTITIONS:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.step.context.topic.replication.factor=${KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.message.parameters.topic.name=${KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.message.parameters.topic.partitions.number=${KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC_PARTITIONS:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dkafka.logrecord.message.parameters.topic.replication.factor=${KAFKA_LOGRECORD_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR:?}"

if [ "${MULTI_TENANCY_HIBERNATE_ENABLED}" = "true" ]; then
    i=0
    _javaoptions="javaoptions.$$"
    env | grep -E '^ADDITIONAL_PG_.*_URL=.*' | sort -u | while IFS='=' read -r _key _value; do
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].url=${_value}" >> "${_javaoptions}"
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_USERNAME=.*' | sort -u | while IFS='=' read -r _key _value; do
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].username=${_value}" >> "${_javaoptions}"
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_PASSWORD=.*' | sort -u | while IFS='=' read -r _key _value; do
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].password=${_value}" >> "${_javaoptions}"
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_DRIVER=.*' | sort -u | while IFS='=' read -r _key _value; do
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].driver-class=${_value}" >> "${_javaoptions}"
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_PROJECTS=.*' | sort -u | while IFS='=' read -r _key _value; do
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].projects=${_value}" >> "${_javaoptions}"
    i=$((i+1))
    done

    sort -u -o "${_javaoptions}" "${_javaoptions}"
    while read -r _line; do
      JAVA_OPTIONS="${JAVA_OPTIONS} ${_line}"
    done <"${_javaoptions}"
fi

/usr/bin/java -Xverify:none -Xms128m -XX:MaxRAM="${MAX_RAM_SIZE:-4000m}" -XX:MaxRAMPercentage=75.0 ${JAVA_OPTIONS} -cp "./:./lib/*" org.qubership.automation.itf.Main
