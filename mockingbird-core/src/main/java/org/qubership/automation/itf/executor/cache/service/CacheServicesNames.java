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

package org.qubership.automation.itf.executor.cache.service;

public interface CacheServicesNames {

    String TC_CONTEXT_CACHE_SERVICE = "tcContextCacheService";
    String BOUND_CONTEXTS_CACHE_SERVICE = "boundContextsCacheService";
    String PENDING_DATA_CONTEXTS_CACHE_SERVICE = "pendingDataContextsCacheService";
    String RUNNING_SCHEDULED_TASKS_CACHE_SERVICE = "runningScheduledTasksCacheService";
    String AWAITING_CONTEXTS_CACHE_SERVICE = "awaitingContextCacheService";
    String CALLCHAIN_SUBSCRIBER_CACHE_SERVICE = "callchainSubscriberCacheService";
    String ENVIRONMENT_CACHE_SERVICE = "environmentService";
    String RESPONSE_CACHE_SERVICE = "responseCacheService";
}
