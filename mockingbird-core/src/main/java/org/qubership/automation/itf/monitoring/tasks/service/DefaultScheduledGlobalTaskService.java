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

package org.qubership.automation.itf.monitoring.tasks.service;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.qubership.automation.itf.executor.cache.service.RunningScheduledTasksCacheService;
import org.qubership.automation.itf.monitoring.tasks.RunningScheduledTask;
import org.qubership.automation.itf.monitoring.tasks.ScheduledGlobalTaskKey;
import org.qubership.automation.itf.monitoring.tasks.ScheduledTaskSettings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DefaultScheduledGlobalTaskService implements ScheduledTaskService {

    private final ScheduledExecutorService scheduledExecutor;
    private final RunningScheduledTasksCacheService runningScheduledTasksCacheService;

    protected DefaultScheduledGlobalTaskService(ScheduledExecutorService scheduledExecutor,
                                                RunningScheduledTasksCacheService runningScheduledTasksCacheService) {
        this.scheduledExecutor = scheduledExecutor;
        this.runningScheduledTasksCacheService = runningScheduledTasksCacheService;
    }

    /**
     * Method will schedule a global task for ITF instance without binding with projects.
     * <br><br>
     * {@link ScheduledTaskSettings} object is used for schedule task
     * with project settings (delay, possibility of scheduling).
     * <br><br>
     * IMPORTANT: functionality of the cache with running tasks
     * was implemented in {@link RunningScheduledTasksCacheService}
     * to support multi-replica ITF instance.
     * The current executive task will not be duplicated when it is scheduled and run on another replica.
     */
    public void scheduleTask() {
        ScheduledTaskSettings taskSettings = createScheduledTaskSettings();
        scheduleTaskIfPossible(taskSettings);
    }

    private void scheduleTaskIfPossible(ScheduledTaskSettings taskSettings) {
        if (!taskSettings.isSchedulingPossible()) {
            log.debug("Task {} can not be scheduled due to condition. "
                    + "Task will not run!", getTaskType());
        } else {
            scheduledExecutor.scheduleWithFixedDelay(
                    this::runTaskIfNotAlreadyRunning,
                    taskSettings.getInitialDelay(),
                    taskSettings.getDelay(),
                    taskSettings.getDelayTimeUnit());
            log.info("Task {} was scheduled with next settings: {}.", getTaskType(), taskSettings);
        }
    }

    private void runTaskIfNotAlreadyRunning() {
        ScheduledGlobalTaskKey taskKey = new ScheduledGlobalTaskKey(getTaskType());
        Optional<RunningScheduledTask> runningTask = runningScheduledTasksCacheService.findByScheduledTaskKey(taskKey);
        if (runningTask.isPresent()) {
            log.debug("Global task {} already running since {}. Duplicate task will not run!",
                    getTaskType(), runningTask.get().getStartDate());
        } else {
            runningScheduledTasksCacheService.storeRunningTaskToCache(taskKey);
            runTask();
            runningScheduledTasksCacheService.evictRunningTaskFromCache(taskKey);
        }
    }

    protected abstract void runTask();

    protected abstract ScheduledTaskSettings createScheduledTaskSettings();
}
