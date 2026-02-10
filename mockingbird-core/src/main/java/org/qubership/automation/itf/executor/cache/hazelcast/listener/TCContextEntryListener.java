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

import javax.annotation.Nonnull;

import org.apache.logging.log4j.util.Strings;
import org.qubership.automation.itf.core.metric.MetricsAggregateService;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.config.Config;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.internal.serialization.Data;
import com.hazelcast.map.impl.DataAwareEntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TCContextEntryListener implements
        EntryAddedListener<Object, TcContext>,
        EntryUpdatedListener<Object, TcContext>,
        EntryEvictedListener<Object, TcContext> {

    private Integer MAX_SIZE;

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

    private boolean isContextCreatedOnThisPod(@Nonnull TcContext tcContext) {
        return Strings.isNotEmpty(tcContext.getPodName())
                && tcContext.getPodName().equals(Config.getConfig().getRunningHostname());
    }

    private void collectContextSizeMetric(EntryEvent entryEvent) {
        if (MAX_SIZE == null) {
            MAX_SIZE = ApplicationConfig.env.getProperty("hazelcast.context.maxSize.metrics.threshold", Integer.class);
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
