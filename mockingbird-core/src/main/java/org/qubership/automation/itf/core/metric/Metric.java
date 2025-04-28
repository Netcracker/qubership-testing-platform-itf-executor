/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.core.metric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Metric {

    ATP_ITF_EXECUTOR_CALLCHAIN_COUNT_BY_PROJECT("atp_itf_executor_callchain_count_by_project"),
    ATP_ITF_EXECUTOR_CALLCHAIN_SECONDS_BY_PROJECT("atp_itf_executor_callchain_seconds_by_project"),
    ATP_ITF_EXECUTOR_STUB_REQUEST_SECONDS_BY_PROJECT("atp_itf_executor_stub_request_processing_by_project"),
    ATP_ITF_EXECUTOR_CONTEXT_SIZE_BY_PROJECT("atp_itf_executor_context_size_by_project"),
    ATP_ITF_EXECUTOR_JMS_LISTENER_THREAD_POOL_MAX_SIZE("atp_itf_executor_jms_listener_thread_pool_max_size"),
    ATP_ITF_EXECUTOR_JMS_LISTENER_THREAD_POOL_ACTIVE_SIZE("atp_itf_executor_jms_listener_thread_pool_active_size"),
    ATP_ITF_EXECUTOR_REGULAR_POOL_MAX_SIZE("atp_itf_executor_regular_pool_max_size"),
    ATP_ITF_EXECUTOR_REGULAR_POOL_ACTIVE_SIZE("atp_itf_executor_regular_pool_active_size"),
    ATP_ITF_EXECUTOR_INBOUND_POOL_MAX_SIZE("atp_itf_executor_inbound_pool_max_size"),
    ATP_ITF_EXECUTOR_INBOUND_POOL_ACTIVE_SIZE("atp_itf_executor_inbound_pool_active_size");
    private final String value;

}
