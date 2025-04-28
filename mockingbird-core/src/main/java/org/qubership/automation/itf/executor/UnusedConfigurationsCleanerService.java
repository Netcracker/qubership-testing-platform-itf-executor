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

package org.qubership.automation.itf.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.qubership.automation.itf.core.hibernate.spring.managers.executor.ServerObjectManager;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UnusedConfigurationsCleanerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnusedConfigurationsCleanerService.class);
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final long initialDelay = 180L;
    private final long delay = 60 * 60 * 24 * 2L;
    private boolean stopped = false;

    public void startWorker() {
        LOGGER.info("Unused Configuration Removing Service is started.");
        service.scheduleWithFixedDelay(this::deleteUnusedOutboundConfigurations, initialDelay, delay, TimeUnit.SECONDS);
    }

    public void stop() {
        LOGGER.info("Worker is stopped.");
        service.shutdown();
        stopped = true;
    }

    private void deleteUnusedOutboundConfigurations() {
        final String activityName = "Removing of Unused Outbound Configurations by schedule";
        TxExecutor.executeUnchecked((Callable<Void>) () -> {
            try {
                LOGGER.info("{} is started...", activityName);
                int deletedCount = CoreObjectManager.getInstance()
                        .getSpecialManager(Server.class, ServerObjectManager.class)
                        .deleteUnusedOutboundConfigurations();
                LOGGER.info("{} is completed ({} rows deleted).", activityName, deletedCount);
            } catch (Throwable t) {
                LOGGER.error("Error while {}", activityName, t);
            }
            return null;
        }, TxExecutor.defaultWritableTransaction());
    }
}
