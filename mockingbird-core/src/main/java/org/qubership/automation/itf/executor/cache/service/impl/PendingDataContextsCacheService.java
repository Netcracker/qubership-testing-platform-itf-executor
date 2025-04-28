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

import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_PENDING_DATA_CONTEXTS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.util.manager.MonitorManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PendingDataContextsCacheService {

    @Autowired
    PendingDataContextsCacheService pendingDataContextsCacheService;
    private HazelcastInstance hazelcastClient;

    @Autowired
    public void setHazelcastClient(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        this.hazelcastClient = hazelcastClient;
    }

    public void addContext(TcContext tcContext, String key) {
        Object contextId = tcContext.getID();
        Object locObject = MonitorManager.getInstance().get("$id=" + contextId);
        synchronized (locObject) {
            Map<Object, TcContext> pendingContext = pendingDataContextsCacheService.getContextById(contextId);
            pendingContext.put(key, tcContext);
            pendingDataContextsCacheService.set(contextId, pendingContext);
        }
    }

    public Map<Object, TcContext> getContextById(Object contextId) {
        Map<Object, TcContext> map = getPendingDataContexts().get(contextId);
        return map == null
                ? new HashMap<>()
                : map;
    }

    public void clearPendingDataContext(Object contextId) {
        getPendingDataContexts().evict(contextId);
    }

    private void set(Object contextId, Map<Object, TcContext> pendingDataContext) {
        getPendingDataContexts().set(contextId, pendingDataContext, 3, TimeUnit.MINUTES);
    }

    private IMap<Object, Map<Object, TcContext>> getPendingDataContexts() {
        return hazelcastClient.getMap(ATP_ITF_PENDING_DATA_CONTEXTS);
    }
}
