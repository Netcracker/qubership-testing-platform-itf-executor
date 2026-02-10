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
import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BoundContextsCacheService {

    @Autowired
    BoundContextsCacheService boundContextsCacheService;
    private HazelcastInstance hazelcastClient;

    @Autowired
    public void setHazelcastClient(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        this.hazelcastClient = hazelcastClient;
    }

    /**
     * Bind the context to the ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache for each key and put (set) context to
     * ATP_ITF_TC_CONTEXTS cache.
     *
     * @param tcContext - TcContext
     */
    public void bind(TcContext tcContext) {
        for (String key : tcContext.getBindingKeys()) {
            boundContextsCacheService.set(key, tcContext);
        }
    }

    /**
     * Bind the context to the ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache
     * and also putIfAbsent it to ATP_ITF_TC_CONTEXTS cache only in case the key is added.
     *
     * @param key     - binding key
     * @param context - TcContext
     * @return true if bound, false if not bound
     */
    public boolean bind(String key, TcContext context) {
        if (context.getBindingKeys().add(key)) {
            boundContextsCacheService.set(key, context);
            return true;
        }
        return false;
    }

    /**
     * Unbind contexts (evict) from the ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache for each key.
     *
     * @param tcContext - TcContext
     */
    public void unbind(TcContext tcContext) {
        String prefix = getPrefix(tcContext, "unbind");
        for (String key : tcContext.getBindingKeys()) {
            boundContextsCacheService.evict(prefix + key);
        }
    }

    /**
     * Find TcContext by key in cache. If no context found in cache, it will be created
     * via createByKey(String, boolean, boolean)
     *
     * @param key - String context key in bindingCache
     * @return - TcContext from ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache
     *     or created (and put to ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache)
     */
    public TcContext findByKey(String key, BigInteger projectId, UUID projectUuid) {
        return findByKey(key, true, projectId, projectUuid);
    }

    /**
     * Find TcContextId by key in ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache.
     * If no contextId found in cache, it will be created
     * via createByKey(String, boolean, boolean), ONLY IF createIfNotFound = true.
     * Otherwise, null is returned.
     *
     * @param key              - String context key in ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache.
     * @param createIfNotFound - "true" if you need to create new context if it is not found in cache or "false".
     * @param projectId        - internal project id.
     * @param projectUuid      - external project uuid.
     * @return - TcContext object from ATP_ITF_TC_CONTEXTS cache or new created in memory that was set to
     *     ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY and ATP_ITF_TC_CONTEXTS caches after creation.
     */
    public TcContext findByKey(String key, boolean createIfNotFound, BigInteger projectId, @Nonnull UUID projectUuid) {
        String keyPrefix = projectUuid.toString() + '/';
        Object tcContextId = boundContextsCacheService.getBoundContextsCache().get(keyPrefix + key);
        if (Objects.isNull(tcContextId)) {
            log.debug("Context id not found by key {} in {} cache for project {}. createIfNotFound = {}", key,
                    ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY, projectUuid, createIfNotFound);
            return (createIfNotFound)
                    ? boundContextsCacheService.createByKey(key, false, projectId, projectUuid)
                    : null;
        }
        TcContext tcContext = CacheServices.getTcContextCacheService().getById(tcContextId);
        if (Objects.isNull(tcContext)) {
            log.debug("Context not found by contextId {} in {} cache for project {}. createIfNotFound = {}",
                    tcContextId, ATP_ITF_TC_CONTEXTS, projectUuid, createIfNotFound);
            return (createIfNotFound)
                    ? boundContextsCacheService.createByKey(key, false, projectId, projectUuid)
                    : null;
        }
        return tcContext;
    }

    /**
     * Find TcContext by keys (get context id from ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY cache
     * and get context object from ATP_ITF_TC_CONTEXTS).
     * If no context found in cache, it will be created via createByKeys(String, boolean, boolean),
     * bind to ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY and put (set) to ATP_ITF_TC_CONTEXTS cache for each key.
     *
     * @param keys        keys
     * @param projectId   internal projectId
     * @param projectUuid external project uuid
     * @return found TcContext or new created object
     */
    public TcContext findByKeys(String[] keys, BigInteger projectId, UUID projectUuid) {
        TcContext context;
        Arrays.sort(keys);
        for (String key : keys) {
            context = boundContextsCacheService.findByKey(key, false, projectId, projectUuid);
            if (context != null) {
                log.debug("Context is found by key {}", key);
                return context;
            }
        }
        log.info("No context by keys {} found, will be created...", Arrays.toString(keys));
        context = boundContextsCacheService.createByKeys(keys, false, projectId, projectUuid);
        return context;
    }

    public TcContext createByKey(String key, boolean isStub, BigInteger projectId, UUID projectUuid) {
        TcContext context = ExecutionServices.getTCContextService().createInMemory(projectId, projectUuid);
        boundContextsCacheService.addIfNotBlank(context, key, isStub);
        log.info("A new context is created with key {} (prefix {}). isStub == {}", key, projectUuid, isStub);
        return context;
    }

    public TcContext createByKeys(String[] keys, boolean isStub, BigInteger projectId, UUID projectUuid) {
        TcContext context = ExecutionServices.getTCContextService().createInMemory(projectId, projectUuid);
        for (String key : keys) {
            boundContextsCacheService.addIfNotBlank(context, key, isStub);
        }
        log.info("A new context {} is created with keys {}", context.getID(), Arrays.toString(keys));
        return context;
    }

    private void set(String key, TcContext context) {
        String keyPrefix = getPrefix(context, "set");
        boundContextsCacheService.getBoundContextsCache().set(keyPrefix + key, context.getID());
        log.debug("Key {} was set to {} cache", keyPrefix + key, ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY);
        CacheServices.getTcContextCacheService().set(context, false);
    }

    private void evict(String key) {
        boundContextsCacheService.getBoundContextsCache().evict(key);
        log.info("Key {} was unbind from {} cache", key, ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY);
    }

    private IMap<Object, Object> getBoundContextsCache() {
        return hazelcastClient.getMap(ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY);
    }

    private void addIfNotBlank(TcContext context, String key, boolean isStub) {
        if (!StringUtils.isBlank(key)) {
            context.getBindingKeys().add(key);
            if (!isStub) {
                boundContextsCacheService.set(key, context);
            }
        }
    }

    private String getPrefix(@Nonnull TcContext context, String action) {
        UUID projectUuid = context.getProjectUuid();
        if (Objects.nonNull(projectUuid)) {
            return projectUuid.toString() + '/';
        }
        throw new IllegalArgumentException(
                String.format("Can't %s TcContext to/from %s cache. Key prefix (projectUuid) is null for TcContext %s ",
                        action, ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY, context.getID()));
    }
}
