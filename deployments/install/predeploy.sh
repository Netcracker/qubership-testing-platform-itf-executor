#!/usr/bin/env sh

#get value of dynamic variable
getValue() {
  eval "value=\"\${$1}\""
  echo "${value}"
}

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh"
  exit 1
fi

# shellcheck source=../atp-common-scripts/openshift/common.sh
. ./atp-common-scripts/openshift/common.sh

_ns="${NAMESPACE}"
echo "NAMESPACE"
echo ${NAMESPACE}
echo "***** Preparing Postgres connection *****"
ITF_EXECUTOR_DB="$(env_default "${ITF_EXECUTOR_DB}" "${SERVICE_NAME}" "${_ns}")"
ITF_EXECUTOR_DB_USER="$(env_default "${ITF_EXECUTOR_DB_USER}" "${SERVICE_NAME}" "${_ns}")"
ITF_EXECUTOR_DB_PASSWORD="$(env_default "${ITF_EXECUTOR_DB_PASSWORD}" "${SERVICE_NAME}" "${_ns}")"

# shellcheck disable=SC2154
init_pg "${PG_DB_ADDR}" "${ITF_EXECUTOR_DB}" "${ITF_EXECUTOR_DB_USER}" "${ITF_EXECUTOR_DB_PASSWORD}" "${PG_DB_PORT}" "${pg_user}" "${pg_pass}"

if [ "${MULTI_TENANCY_HIBERNATE_ENABLED:-false}" = "true" ]; then
  echo "Multi-tenancy-hibernate is enabled"
  _clusters="$(env | grep -e 'ADDITIONAL_.*CLUSTER.*_URL'|cut -d _ -f3|sort)"
  if [ -n "${_clusters}" ]; then
  echo "Additional clusters:"
  for cluster in ${_clusters}
    do
      _url="ADDITIONAL_PG_${cluster}_URL"
      _user="ADDITIONAL_PG_${cluster}_USERNAME"
      _password="ADDITIONAL_PG_${cluster}_PASSWORD"
      echo "${cluster} :=>  $(getValue "$_url")"
      ADDITIONAL_DB_HOST=$(getValue "$_url"|awk -F ':' '{print $3}'|sed 's/\/\///g')
      ADDITIONAL_DB_NAME=$(getValue "$_url"|awk -F ":" '{print $4}'|cut -d / -f2)
      ADDITIONAL_DB_PORT=$(getValue "$_url"|awk -F ":" '{print $4}'|cut -d / -f1)
      ADDITIONAL_DB_USER=$(getValue "$_user")
      ADDITIONAL_DB_PASSWORD=$(getValue "$_password")
      init_pg "${ADDITIONAL_DB_HOST:?}" "${ADDITIONAL_DB_NAME:?}" \
              "${ADDITIONAL_DB_USER:?}" "${ADDITIONAL_DB_PASSWORD:?}" \
              "${ADDITIONAL_DB_PORT:?}" "${pg_user}" "${pg_pass}" < /dev/null
    done
  else
    echo "List of additional clusters is empty."
  fi
else
  echo "Multi-tenancy-hibernate is disabled"
fi

if [ "${EDS_GRIDFS_ENABLED:-true}" = "true" ]; then
  echo "***** Preparing MongoDB connection *****"
  EDS_GRIDFS_DB="$(env_default "${EDS_GRIDFS_DB}" "itf_eds_gridfs" "${_ns}")"
  EDS_GRIDFS_USER="$(env_default "${EDS_GRIDFS_USER}" "itf_eds_gridfs" "${_ns}")"
  EDS_GRIDFS_PASSWORD="$(env_default "${EDS_GRIDFS_PASSWORD}" "itf_eds_gridfs" "${_ns}")"

  # shellcheck disable=SC2154
  init_mongo "${MONGO_DB_ADDR}" "${EDS_GRIDFS_DB}" "${EDS_GRIDFS_USER}" "${EDS_GRIDFS_PASSWORD}" "${MONGO_DB_PORT}"  "${mongo_user}" "${mongo_pass}"
fi

if [ "${EI_GRIDFS_ENABLED:-true}" = "true" ]; then
  echo "***** Preparing Gridfs connection *****"
  EI_GRIDFS_DB="$(env_default "${EI_GRIDFS_DB}" "atp-ei-gridfs" "${_ns}")"
  EI_GRIDFS_USER="$(env_default "${EI_GRIDFS_USER}" "atp-ei-gridfs" "${_ns}")"
  EI_GRIDFS_PASSWORD="$(env_default "${EI_GRIDFS_PASSWORD}" "atp-ei-gridfs" "${_ns}")"

  # shellcheck disable=SC2154
  init_mongo "${EI_GRIDFS_DB_ADDR:-$GRIDFS_DB_ADDR}" "${EI_GRIDFS_DB}" "${EI_GRIDFS_USER}" "${EI_GRIDFS_PASSWORD}" "${EI_GRIDFS_DB_PORT:-$GRIDFS_DB_PORT}" "${ei_gridfs_user}" "${ei_gridfs_pass}"
fi
