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

package org.qubership.automation.itf.executor.cache.hazelcast.listener;

import java.util.Objects;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.metric.MetricsAggregateService;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.MDC;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.internal.serialization.Data;
import com.hazelcast.map.impl.DataAwareEntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryExpiredListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TCContextEntryListener implements EntryAddedListener<Object, TcContext>,
        EntryUpdatedListener<Object, TcContext>, EntryExpiredListener<Object, TcContext>,
        EntryEvictedListener<Object, TcContext> {

    private Boolean multiTenancyEnabled;
    private Integer MAX_SIZE;

    /**
     * This method calls only when expire event comes from Hazelcast service for tc context to all
     * atp-itf-executor pods.
     *
     * @param entryEvent that contains expired tcContext.
     */
    @Override
    public void entryExpired(EntryEvent entryEvent) {
        collectContextSizeMetric(entryEvent);
        try {
            if (multiTenancyEnabled == null) {
                multiTenancyEnabled = ApplicationConfig.env.getProperty("atp.multi-tenancy.enabled", Boolean.class);
            }
            TcContext tcContext = (TcContext) entryEvent.getOldValue();
            String projectUuid = tcContext.getProjectUuid().toString();
            MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
            MdcUtils.put(MdcField.CONTEXT_ID.toString(), tcContext.getID().toString());
            if (multiTenancyEnabled) {
                TenantContext.setTenantInfo(projectUuid);
            }
            if (!isContextCreatedOnThisPod(tcContext)) {
                log.info("Context initiated on another pod. "
                        + "TerminateByTimeout process is skipped on this pod.");
                return;
            }
            if (tcContext.getInitiator() instanceof SituationInstance && isContextUpdatedByDifferentPods(tcContext)) {
                log.info("Context was updated and expiration time for this context is changed on "
                        + "another pod before this expire event is got. "
                        + "TerminateByTimeout process is skipped on this pod ({}).", tcContext.getPodName());
                return;
            }
            log.info("TerminateByTimeout process is started for context {}", tcContext.getID());
            ExecutionServices.getTCContextService().terminateByTimeout(tcContext);
            log.info("TerminateByTimeout process is finished for context {}", tcContext.getID());
        } catch (Exception e) {
            log.error("Error while trying to terminate tc context by timeout", e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * This method calls only when added event comes from Hazelcast service for tc context to all
     * atp-itf-executor pods.
     *
     * @param entryEvent that contains added tcContext.
     */
    @Override
    public void entryAdded(EntryEvent entryEvent) {
        collectContextSizeMetric(entryEvent);
    }

    /**
     * This method calls only when updated event comes from Hazelcast service for tc context to all
     * atp-itf-executor pods.
     *
     * @param entryEvent that contains updated tcContext.
     */
    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        collectContextSizeMetric(entryEvent);
    }

    /**
     * This method calls only when evicted event comes from Hazelcast service for tc context to all
     * atp-itf-executor pods.
     *
     * @param entryEvent that contains evicted tcContext.
     */
    @Override
    public void entryEvicted(EntryEvent entryEvent) {
        collectContextSizeMetric(entryEvent);
    }

    private boolean isContextCreatedOnThisPod(@NotNull TcContext tcContext) {
        return Strings.isNotEmpty(tcContext.getPodName())
                && tcContext.getPodName().equals(Config.getConfig().getRunningHostname());
    }

    private boolean isContextUpdatedByDifferentPods(TcContext tcContext) {
        long expirationTime = CacheServices.getTcContextCacheService().getExpirationTime(tcContext);
        return expirationTime != 0 && expirationTime >= System.currentTimeMillis();
    }

    private void collectContextSizeMetric(EntryEvent entryEvent) {
        if (MAX_SIZE == null) {
            MAX_SIZE = ApplicationConfig.env.getProperty("hazelcast.context.maxSize.metrics", Integer.class);
        }
        try {
            switch (entryEvent.getEventType()) {
                case ADDED:
                case UPDATED:
                    TcContext newTcContext = (TcContext) entryEvent.getValue();
                    if (!isContextCreatedOnThisPod(newTcContext)) {
                        return;
                    }
                    Data newValueData = ((DataAwareEntryEvent) entryEvent).getNewValueData();
                    if (Objects.nonNull(newValueData) && newValueData.totalSize() > MAX_SIZE) {
                        MetricsAggregateService.summaryHazelcastContextSizeCountToProject(
                                newTcContext.getProjectUuid(), entryEvent.getKey(), newValueData.totalSize());
                    }
                    break;
                case EXPIRED:
                case EVICTED:
                    TcContext oldTcContext = (TcContext) entryEvent.getOldValue();
                    if (!isContextCreatedOnThisPod(oldTcContext)) {
                        return;
                    }
                    MetricsAggregateService.removeHazelcastContextSizeCountToProject(
                            oldTcContext.getProjectUuid(), entryEvent.getKey());
                    break;
                default:
            }
        } catch (Exception e) {
            log.error("Error while trying to collect context size metric", e);
        }
    }
}
