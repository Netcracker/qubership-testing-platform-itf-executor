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

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.MDC;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryExpiredListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TCContextEntryExpiredListener implements EntryExpiredListener<Object, TcContext> {

    private static final String LOG_MESSAGE_TEMPLATE = "TerminateByTimeout process for context";
    private Boolean multiTenancyEnabled;

    /**
     * This method calls only when expire event comes from Hazelcast service for tc context to all
     * atp-itf-executor pods.
     *
     * @param entryEvent that contains expired tcContext.
     */
    @Override
    public void entryExpired(EntryEvent entryEvent) {
        try {
            /* Instead of deserializing of each event's value just to determine if the corresponding TcContext ran
                on this pod, let's check it via TCContextService.localRunningContexts cache (since 2025-12-18).
                The problem (why was this local cache introduced?) was:
                    - 18 pods of itf-executor service were running in parallel,
                    - So, EntryExpired events were relating TcContexts from all these pods,
                    - To determine, if this TcContext is own-or-alien, we deserialized all received TcContexts,
                    - So, each pod of the service performed such deserialization against events related all 18 pods(!)
                    - It was very resource consuming. And, it was synchronous processing.
             */
            BigInteger id = (BigInteger) entryEvent.getKey();
            TcContext tcContext = ExecutionServices.getTCContextService().getLocalRunningContext(id);
            if (tcContext == null) {
                log.info("Context initiated on another pod. TerminateByTimeout process is skipped on this pod.");
                return;
            }

            // Submit task for async processing, in order to quickly make listener available for other events
            CompletableFuture.runAsync(() ->
                            processEntryExpiredEvent(tcContext),
                    ExecutionServices.getTCContextService().getHazelcastAsyncExecutor().getAsyncTasksPool());
            log.info("{} {} is queued", LOG_MESSAGE_TEMPLATE, id);
        } catch (Exception e) {
            log.error("Error while trying to process entryExpired event", e);
        }
    }

    private void processEntryExpiredEvent(TcContext tcContext) {
        try {
            String projectUuid = tcContext.getProjectUuid().toString();
            MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
            MdcUtils.put(MdcField.CONTEXT_ID.toString(), tcContext.getID().toString());

            if (multiTenancyEnabled == null) {
                multiTenancyEnabled = ApplicationConfig.env.getProperty("atp.multi-tenancy.enabled", Boolean.class);
            }
            if (Boolean.TRUE.equals(multiTenancyEnabled)) {
                TenantContext.setTenantInfo(projectUuid);
            }

            // Especially for contexts bound by a key, which could be executed on more than one pod,
            // check if the context is already processed on another pod.
            if (tcContext.getInitiator() instanceof SituationInstance && isContextUpdatedByDifferentPods(tcContext)) {
                log.warn("Context was updated and expiration time for this context is changed on "
                        + "another pod before this expire event is got. "
                        + "So, TerminateByTimeout process is skipped on this pod ({}).", tcContext.getPodName());
                return;
            }
            log.info("{} {} is started", LOG_MESSAGE_TEMPLATE, tcContext.getID());
            ExecutionServices.getTCContextService().terminateByTimeout(tcContext);
            log.info("{} {} is finished", LOG_MESSAGE_TEMPLATE, tcContext.getID());
        } catch (Exception e) {
            log.error("Error while trying to terminate tc context by timeout", e);
        } finally {
            MDC.clear();
        }
    }

    private boolean isContextUpdatedByDifferentPods(TcContext tcContext) {
        long expirationTime = CacheServices.getTcContextCacheService().getExpirationTime(tcContext);
        return expirationTime != 0 && expirationTime >= System.currentTimeMillis();
    }

}
