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

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.cache.service.RunningScheduledTasksCacheService;
import org.qubership.automation.itf.monitoring.tasks.RunningScheduledTask;
import org.qubership.automation.itf.monitoring.tasks.ScheduledProjectTaskKey;
import org.qubership.automation.itf.monitoring.tasks.ScheduledTaskSettings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DefaultScheduledProjectTaskService implements ScheduledTaskService {

    private final ScheduledExecutorService scheduledExecutor;
    private final RunningScheduledTasksCacheService runningScheduledTasksCacheService;

    protected DefaultScheduledProjectTaskService(ScheduledExecutorService scheduledExecutor,
                                                 RunningScheduledTasksCacheService runningScheduledTasksCacheService) {
        this.scheduledExecutor = scheduledExecutor;
        this.runningScheduledTasksCacheService = runningScheduledTasksCacheService;
    }

    /**
     * Method will schedule a separate project task for all projects.
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
        CoreObjectManager.getInstance()
                .getManager(StubProject.class)
                .getAll()
                .forEach(project -> {
                    BigInteger projectId = (BigInteger) project.getID();
                    ScheduledTaskSettings taskSettings = createScheduledTaskSettingsForProject(projectId);
                    scheduleTaskIfPossible(taskSettings, projectId);
                });
    }

    private void scheduleTaskIfPossible(ScheduledTaskSettings taskSettings, BigInteger projectId) {
        if (!taskSettings.isSchedulingPossible()) {
            log.debug("Task {} for project {} can not be scheduled due to condition. "
                    + "Task will not run!", getTaskType(), projectId);
        } else {
            scheduledExecutor.scheduleWithFixedDelay(
                    () -> runTaskIfNotAlreadyRunning(projectId),
                    taskSettings.getInitialDelay(),
                    taskSettings.getDelay(),
                    taskSettings.getDelayTimeUnit());
            log.info("Task {} for project {} was scheduled with next settings: {}.",
                    getTaskType(), projectId, taskSettings);
        }
    }

    private void runTaskIfNotAlreadyRunning(BigInteger projectId) {
        ScheduledProjectTaskKey taskKey = new ScheduledProjectTaskKey(getTaskType(), projectId);
        Optional<RunningScheduledTask> runningTask = runningScheduledTasksCacheService.findByScheduledTaskKey(taskKey);
        if (runningTask.isPresent()) {
            log.debug("Task {} already running for project {} since {}. Duplicate task will not run!",
                    getTaskType(), projectId, runningTask.get().getStartDate());
        } else {
            runningScheduledTasksCacheService.storeRunningTaskToCache(taskKey);
            runTaskForProject(projectId);
            runningScheduledTasksCacheService.evictRunningTaskFromCache(taskKey);
        }
    }

    protected abstract void runTaskForProject(BigInteger projectId);

    protected abstract ScheduledTaskSettings createScheduledTaskSettingsForProject(BigInteger projectId);
}
