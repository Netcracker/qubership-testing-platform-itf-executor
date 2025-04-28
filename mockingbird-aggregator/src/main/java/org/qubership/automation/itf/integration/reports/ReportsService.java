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

package org.qubership.automation.itf.integration.reports;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportsService {

    private final ReportsFeignClient reportsFeignClient;
    private final TCContextService tcContextService;
    private final ScheduledExecutorService refreshPartitionsService = initRefreshPartitionsService();

    private ScheduledExecutorService initRefreshPartitionsService() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(() -> {
            try {
                tcContextService.refreshPartitionNumbers(getCurrentPartitionNumbers());
            } catch (Throwable t) {
                log.error("Error while refreshing of current partition numbers from reporting service", t);
            }
        }, 5, 3600, TimeUnit.SECONDS);
        return service;
    }

    public List<Object[]> getContextProperties(String contextId, UUID projectUuid) {
        List<List<Object>> contextPropertiesList =
                reportsFeignClient.getContextProperties(contextId, projectUuid).getBody();
        return Objects.requireNonNull(contextPropertiesList).stream()
                .map(List::toArray)
                .collect(Collectors.toList());
    }

    public String getContextVariables(String contextId, UUID projectUuid) {
        return reportsFeignClient.getContextVariables(contextId, projectUuid).getBody();
    }

    public Set<String> getKeys(String contextId, UUID projectUuid) {
        return Sets.newHashSet(reportsFeignClient.getKeys(contextId, projectUuid).getBody());
    }

    public Map<String, Integer> getCurrentPartitionNumbers() {
        return reportsFeignClient.getCurrentPartitionNumbers().getBody();
    }

}
