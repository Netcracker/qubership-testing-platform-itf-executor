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

import java.util.HashMap;
import java.util.Map;

import org.qubership.automation.itf.executor.cache.service.impl.AwaitingContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.BoundContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.CallchainSubscriberCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.EnvironmentCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.PendingDataContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.ResponseCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.TCContextCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CacheServices {

    private static final Map<String, Object> executorCacheServiceMap = new HashMap<>();

    /**
     * Cache services initializer.
     *
     * @param tcContextCacheService           tcContextCacheService.
     * @param boundContextsCacheService       boundContextsCacheService.
     * @param pendingDataContextsCacheService pendingDataContextsCacheService.
     * @param awaitingContextsCacheService    awaitingContextsCacheService.
     * @param callchainSubscriberCacheService callchainSubscriberCacheService.
     * @param environmentCacheService         environmentCacheService.
     */
    @Autowired
    public CacheServices(TCContextCacheService tcContextCacheService,
                         BoundContextsCacheService boundContextsCacheService,
                         PendingDataContextsCacheService pendingDataContextsCacheService,
                         AwaitingContextsCacheService awaitingContextsCacheService,
                         CallchainSubscriberCacheService callchainSubscriberCacheService,
                         EnvironmentCacheService environmentCacheService,
                         ResponseCacheService responseCacheService) {
        executorCacheServiceMap.put(CacheServicesNames.TC_CONTEXT_CACHE_SERVICE, tcContextCacheService);
        executorCacheServiceMap.put(CacheServicesNames.BOUND_CONTEXTS_CACHE_SERVICE, boundContextsCacheService);
        executorCacheServiceMap.put(CacheServicesNames.PENDING_DATA_CONTEXTS_CACHE_SERVICE,
                pendingDataContextsCacheService);
        executorCacheServiceMap.put(CacheServicesNames.AWAITING_CONTEXTS_CACHE_SERVICE, awaitingContextsCacheService);
        executorCacheServiceMap.put(CacheServicesNames.CALLCHAIN_SUBSCRIBER_CACHE_SERVICE,
                callchainSubscriberCacheService);
        executorCacheServiceMap.put(CacheServicesNames.ENVIRONMENT_CACHE_SERVICE,
                environmentCacheService);
        executorCacheServiceMap.put(CacheServicesNames.RESPONSE_CACHE_SERVICE,
                responseCacheService);
    }

    public static TCContextCacheService getTcContextCacheService() {
        return (TCContextCacheService) executorCacheServiceMap.get(CacheServicesNames.TC_CONTEXT_CACHE_SERVICE);
    }

    public static BoundContextsCacheService getTcBindingCacheService() {
        return (BoundContextsCacheService) executorCacheServiceMap.get(CacheServicesNames.BOUND_CONTEXTS_CACHE_SERVICE);
    }

    public static PendingDataContextsCacheService getPendingDataContextsCacheService() {
        return (PendingDataContextsCacheService) executorCacheServiceMap.get(
                CacheServicesNames.PENDING_DATA_CONTEXTS_CACHE_SERVICE);
    }

    public static AwaitingContextsCacheService getAwaitingContextsCacheService() {
        return (AwaitingContextsCacheService) executorCacheServiceMap.get(
                CacheServicesNames.AWAITING_CONTEXTS_CACHE_SERVICE);
    }

    public static CallchainSubscriberCacheService getCallchainSubscriberCacheService() {
        return (CallchainSubscriberCacheService) executorCacheServiceMap.get(
                CacheServicesNames.CALLCHAIN_SUBSCRIBER_CACHE_SERVICE);
    }

    public static EnvironmentCacheService getEnvironmentCacheService() {
        return (EnvironmentCacheService) executorCacheServiceMap.get(
                CacheServicesNames.ENVIRONMENT_CACHE_SERVICE);
    }

    public static ResponseCacheService getResponseCacheService() {
        return (ResponseCacheService) executorCacheServiceMap.get(
                CacheServicesNames.RESPONSE_CACHE_SERVICE);
    }
}
