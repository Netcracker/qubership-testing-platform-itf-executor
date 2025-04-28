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

package org.qubership.automation.itf.executor.scheduled;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.StubProjectObjectManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Component
public class CleanUserData {

    private static final String UTC_TIMEZONE = "UTC";
    private final StubProjectObjectManager stubProjectObjectManager;
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;
    @Value("${userdata.clean.leave_days}")
    private long leaveDays;
    @Value("${userdata.clean.leave_days.timeunit}")
    private TimeUnit leaveDaysTimeunit;

    @Autowired
    public CleanUserData(StubProjectObjectManager stubProjectObjectManager) {
        this.stubProjectObjectManager = stubProjectObjectManager;
    }

    /**
     * Job that removes irrelevant data from the user data.
     */
    @Scheduled(cron = "${userdata.clean.job.expression}", zone = UTC_TIMEZONE)
    @SchedulerLock(name = "${userdata.clean.job.name}", lockAtMostFor = "5m")
    public void run() {
        Integer days = Integer.valueOf(String.valueOf(leaveDaysTimeunit.toDays(leaveDays)));
        log.info("Scheduled task execution is started at {}", LocalDateTime.now());
        try {
            if (multiTenancyEnabled) {
                for (String tenantId : TenantContext.getTenantIds(true)) {
                    TenantContext.setTenantInfo(tenantId);
                    stubProjectObjectManager.clearUserData(days);
                }
                TenantContext.setDefaultTenantInfo();
            }
            stubProjectObjectManager.clearUserData(days);
            log.info("Scheduled task execution is finished at {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error while executing of the scheduled task: {}", e.getMessage());
        }
    }
}
