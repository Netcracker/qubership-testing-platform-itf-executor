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

package org.qubership.automation.itf.executor.cache.service.impl;

import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_AWAITING_CONTEXTS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

@Service
public class AwaitingContextsCacheService {

    @Autowired
    AwaitingContextsCacheService awaitingContextsCacheService;
    private HazelcastInstance hazelcastClient;

    @Autowired
    public void setHazelcastClient(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        this.hazelcastClient = hazelcastClient;
    }

    private IMap<Object, Object> getAwaitingContextsCache() {
        return hazelcastClient.getMap(ATP_ITF_AWAITING_CONTEXTS);
    }

    public void putIfAbsent(Object contextId, Object stepInstanceId) {
        awaitingContextsCacheService.getAwaitingContextsCache().putIfAbsent(contextId, stepInstanceId);
    }

    public boolean containsKey(String contextIdAndEndSitId) {
        return awaitingContextsCacheService.getAwaitingContextsCache().containsKey(contextIdAndEndSitId);
    }

    public void evict(Object contextId) {
        awaitingContextsCacheService.getAwaitingContextsCache().evict(contextId);
    }
}
