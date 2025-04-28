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

package org.qubership.automation.itf.executor.cache.hazelcast;

import static org.qubership.automation.itf.core.util.config.Config.getConfig;

import java.util.ArrayList;
import java.util.List;

import org.qubership.automation.itf.core.util.constants.CacheNames;
import org.qubership.automation.itf.executor.cache.hazelcast.listener.DiameterConnectionInfoEvictedListener;
import org.qubership.automation.itf.executor.cache.hazelcast.listener.ResponseEntryExpiredListener;
import org.qubership.automation.itf.executor.cache.hazelcast.listener.TCContextEntryExpiredListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableCaching
@Configuration
public class CommonHazelcastConfig {

    protected static void tryToCreateMapConfigsIfNotExist(HazelcastInstance hazelcastInstance, boolean remoteInstance) {
        Config config = hazelcastInstance.getConfig();
        configCache(config, CacheNames.ATP_ITF_TC_CONTEXTS, true);
        configCache(config, CacheNames.ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY, 7200);
        configCache(config, CacheNames.ATP_ITF_PENDING_DATA_CONTEXTS, 900);
        configCache(config, CacheNames.ATP_ITF_RUNNING_SCHEDULED_TASKS);
        configCache(config, CacheNames.ATP_ITF_AWAITING_CONTEXTS);
        configCache(config, CacheNames.ATP_ITF_DIAMETER_CONNECTION_INFO,
                getConfig().getIntOrDefault("diameter.cacheLifetime", 12) * 3600);
        configCache(config, CacheNames.ATP_ITF_ENVIRONMENT_INFO);
        if (!remoteInstance) {
            configCache(config, CacheNames.ATP_ITF_PROJECT_SETTINGS, true);
        }
        configCache(config, CacheNames.ATP_ITF_RESPONSE_MESSAGES);
    }

    private static void configCache(Config config, String cacheName) {
        configCache(config, cacheName, false);
    }

    private static void configCache(Config config, String cacheName, boolean perEntryStatsEnabled) {
        try {
            logTryToCreateInfo(cacheName);
            config.addMapConfig(new MapConfig()
                    .setName(cacheName)
                    .setPerEntryStatsEnabled(perEntryStatsEnabled)
            );
            logConfigCreatedOrExist(cacheName);
        } catch (Exception exception) {
            log.warn("Map {} already created on Hazelcast cluster side. It's not possible to change existing map "
                    + "config.", cacheName, exception);
        }
    }

    private static void configCache(Config config, String cacheName, int maxIdleSeconds) {
        try {
            logTryToCreateInfo(cacheName);
            config.addMapConfig(new MapConfig().setName(cacheName).setMaxIdleSeconds(maxIdleSeconds));
            logConfigCreatedOrExist(cacheName);
        } catch (Exception exception) {
            log.warn("Map {} already created on Hazelcast cluster side. It's not possible to change existing map "
                    + "config.", cacheName, exception);
        }
    }

    private static void logTryToCreateInfo(String cacheName) {
        log.info("Try to create config for hazelcast {} map.", cacheName);
    }

    private static void logConfigCreatedOrExist(String cacheName) {
        log.info("Config for hazelcast {} map created or already exist with the same parameters.", cacheName);
    }

    /**
     * Create {@link CacheManager} bean.
     *
     * @return bean
     */
    @Bean(name = "hazelcastCacheManager")
    public CacheManager hazelcastCacheManager(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        List<Cache> caches = new ArrayList<>();
        IMap<Object, Object> tcContexts = hazelcastClient.getMap(CacheNames.ATP_ITF_TC_CONTEXTS);
        tcContexts.addEntryListener(new TCContextEntryExpiredListener(), true);
        IMap<Object, Object> boundContexts = hazelcastClient.getMap(CacheNames.ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY);
        IMap<Object, Object> pendingContextData = hazelcastClient.getMap(CacheNames.ATP_ITF_PENDING_DATA_CONTEXTS);
        IMap<Object, Object> runningScheduledTasksCacheMap = hazelcastClient.getMap(
                CacheNames.ATP_ITF_RUNNING_SCHEDULED_TASKS);
        IMap<Object, Object> awaitingContexts = hazelcastClient.getMap(CacheNames.ATP_ITF_AWAITING_CONTEXTS);
        IMap<Object, Object> environments = hazelcastClient.getMap(
                CacheNames.ATP_ITF_ENVIRONMENT_INFO);
        IMap<Object, Object> diameterConnectionInfo = hazelcastClient.getMap(
                CacheNames.ATP_ITF_DIAMETER_CONNECTION_INFO);
        IMap<Object, Object> projectSettings = hazelcastClient.getMap(CacheNames.ATP_ITF_PROJECT_SETTINGS);
        IMap<Object, Object> responseMessages = hazelcastClient.getMap(CacheNames.ATP_ITF_RESPONSE_MESSAGES);
        responseMessages.addEntryListener(new ResponseEntryExpiredListener(), true);
        diameterConnectionInfo.addEntryListener(new DiameterConnectionInfoEvictedListener(), true);
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_DIAMETER_CONNECTION_INFO, diameterConnectionInfo, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_TC_CONTEXTS, tcContexts, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_TC_CONTEXTS_IDS_BOUND_BY_KEY, boundContexts, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_PENDING_DATA_CONTEXTS, pendingContextData, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_RUNNING_SCHEDULED_TASKS, runningScheduledTasksCacheMap,
                false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_AWAITING_CONTEXTS, awaitingContexts, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_ENVIRONMENT_INFO, environments, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_PROJECT_SETTINGS, projectSettings, false));
        caches.add(new ConcurrentMapCache(CacheNames.ATP_ITF_RESPONSE_MESSAGES, responseMessages, false));
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
