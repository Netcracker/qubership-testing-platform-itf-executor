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

package org.qubership.automation.itf.monitoring;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.qubership.automation.itf.monitoring.tasks.SchedulingTasksConfig;
import org.qubership.automation.itf.monitoring.tasks.service.ScheduledTaskService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class TCMonitoringWorker {

    private final ScheduledExecutorService scheduledExecutorService;
    private final List<ScheduledTaskService> scheduledTaskServices;

    /**
     * Constructor for TCMonitoringWorker instance.
     *
     * @param scheduledExecutorService - service for scheduling tasks,
     *                                 configured in {@link SchedulingTasksConfig}
     * @param scheduledTaskServices    - list with all task services (includes project tasks and global tasks)
     */
    //@Autowired
    public TCMonitoringWorker(ScheduledExecutorService scheduledExecutorService,
                              List<ScheduledTaskService> scheduledTaskServices) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.scheduledTaskServices = scheduledTaskServices;
    }

    public void startWorker() {
        scheduledTaskServices.forEach(ScheduledTaskService::scheduleTask);
    }

    public void stop() {
        log.info("Worker is stopped.");
        scheduledExecutorService.shutdown();
    }
}
