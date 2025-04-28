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

import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_RUNNING_SCHEDULED_TASKS;

import java.time.LocalDateTime;
import java.util.Optional;

import org.qubership.automation.itf.monitoring.tasks.RunningScheduledTask;
import org.qubership.automation.itf.monitoring.tasks.ScheduledTaskKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(value = "hazelcast.cache.enabled")
public class RunningScheduledTasksCacheService {

    private final HazelcastInstance hazelcastClient;

    @Autowired
    public RunningScheduledTasksCacheService(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        this.hazelcastClient = hazelcastClient;
    }

    public Optional<RunningScheduledTask> findByScheduledTaskKey(ScheduledTaskKey scheduledTaskKey) {
        return Optional.ofNullable(getRunningTasksCache()
                .get(scheduledTaskKey.getKeyRepresentation()));
    }

    public void storeRunningTaskToCache(ScheduledTaskKey scheduledTaskKey) {
        getRunningTasksCache().put(scheduledTaskKey.getKeyRepresentation(),
                new RunningScheduledTask(LocalDateTime.now()));
    }

    public void evictRunningTaskFromCache(ScheduledTaskKey scheduledTaskKey) {
        getRunningTasksCache().remove(scheduledTaskKey.getKeyRepresentation());
        log.debug("Task {} was evicted from running scheduled tasks cache", scheduledTaskKey);
    }

    private IMap<String, RunningScheduledTask> getRunningTasksCache() {
        return hazelcastClient.getMap(ATP_ITF_RUNNING_SCHEDULED_TASKS);
    }
}
