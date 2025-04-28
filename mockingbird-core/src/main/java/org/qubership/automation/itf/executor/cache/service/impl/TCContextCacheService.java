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

import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_TC_CONTEXTS;
import static org.qubership.automation.itf.core.util.converter.IdConverter.toBigInt;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.hazelcast.core.EntryView;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.HazelcastSerializationException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TCContextCacheService {

    private HazelcastInstance hazelcastClient;

    @Autowired
    public void setHazelcastClient(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        this.hazelcastClient = hazelcastClient;
    }

    public long getLastAccess(TcContext tcContext) {
        EntryView<Object, TcContext> entryView = getTCContextCache().getEntryView(tcContext.getID());
        if (Objects.isNull(entryView)) {
            return System.currentTimeMillis();
        }
        return Math.max(entryView.getLastAccessTime(), entryView.getLastUpdateTime());
    }

    public long getExpirationTime(TcContext tcContext) {
        if (Objects.isNull(tcContext)) {
            log.error("Can't get valid expiration time for tc context.. Tc Context is null. Returned value is 0");
            return 0;
        }
        EntryView<Object, TcContext> entryView = getTCContextCache().getEntryView(tcContext.getID());
        return Objects.isNull(entryView) ? 0 : entryView.getExpirationTime();
    }

    public long getEntryCount() {
        return getTCContextCache().size();
    }

    public TcContext getById(Object contextId) {
        return getTCContextCache().get(toBigInt(contextId));
    }

    public List<TcContext> getAllTcContexts(String podName) {
        List<TcContext> contexts = new LinkedList<>();
        for (Map.Entry<Object, TcContext> contextEntry : getTCContextCache()) {
            if (contextEntry.getValue().getPodName().equals(podName)) {
                contexts.add(contextEntry.getValue());
            }
        }
        return contexts;
    }

    /**
     * Set\putIfAbsent to ATP_ITF_TC_CONTEXTS cache.
     *
     * @param tcContext TcContext object.
     * @param forced    if value is true - this method will use set() without returning old value and serialization,
     *                  if it is false - putIfAbsent() method will be used.
     */
    public void set(TcContext tcContext, boolean forced) {
        long ttl = tcContext.getTimeToLive();
        try {
            if (forced) {
                getTCContextCache().set(tcContext.getID(), tcContext, ttl, TimeUnit.MILLISECONDS);
                log.debug("TcContext {} was set to {} cache. forced=true", tcContext.getID(), ATP_ITF_TC_CONTEXTS);
            } else {
                TcContext oldTcContext = getTCContextCache().putIfAbsent(tcContext.getID(), tcContext, ttl,
                        TimeUnit.MILLISECONDS);
                log.debug(Objects.isNull(oldTcContext)
                                ? "TcContext {} was put to {} cache. forced=false"
                                : "TcContext {} wasn't put to {} cache. Already in cache and forced=false.",
                        tcContext.getID(), ATP_ITF_TC_CONTEXTS);
            }
        } catch (HazelcastSerializationException ex) {
            log.error("Can't serialize TcContext to put/set into ATP_ITF_TC_CONTEXTS.", ex);
        } catch (Exception e) {
            log.error("Something went wrong while set/putIfAbsent TcContext to {} cache", ATP_ITF_TC_CONTEXTS);
        }
    }

    public void evict(TcContext tcContext) {
        getTCContextCache().evict(tcContext.getID());
    }

    private IMap<Object, TcContext> getTCContextCache() {
        return hazelcastClient.getMap(ATP_ITF_TC_CONTEXTS);
    }
}
