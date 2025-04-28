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

package org.qubership.automation.itf.ui.services.javers.history;

import java.time.LocalDateTime;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "javers.history.enabled", havingValue = "true")
public class HistoryOldRevisionsCleaner {

    private static final String UTC_TIMEZONE = "UTC";
    private final DeleteHistoryService deleteHistoryService;

    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;
    @Value("${atp.itf.history.clean.job.revision.max.count}")
    private long maxRevisionCount;
    @Value("${atp.itf.history.clean.job.page-size}")
    private Integer pageSize;

    @Scheduled(cron = "${atp.itf.history.clean.job.expression}", zone = UTC_TIMEZONE)
    @SchedulerLock(name = "${atp.itf.history.clean.job.name}", lockAtMostFor = "5m")
    public void run() {
        log.info("Schedule task: Start execute at {}.", LocalDateTime.now());
        try {
            if (multiTenancyEnabled) {
                for (String tenantId : TenantContext.getTenantIds(true)) {
                    TenantContext.setTenantInfo(tenantId);
                    deleteSnapshots();
                }
                TenantContext.setDefaultTenantInfo();
            }
            deleteSnapshots();
        } catch (Exception e) {
            log.error("Error while executing a scheduled task {}, message: {}.", LocalDateTime.now(), e.getMessage());
        } finally {
            log.info("Schedule task: Finish execute at {}.", LocalDateTime.now());
        }
    }

    private void deleteSnapshots() {
        deleteHistoryService.deleteTerminatedSnapshots(pageSize);
        deleteHistoryService.deleteOldSnapshots(maxRevisionCount);
    }
}
