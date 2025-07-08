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

package org.qubership.automation.itf.executor.service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.DefferedSituationInstanceHolder;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.NextCallChainEventSubscriberHolder;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.SubscriberData;
import org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber;
import org.qubership.automation.itf.core.message.TcContextOperationMessage;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.TcContextEvent;
import org.qubership.automation.itf.core.model.extension.SituationExtension;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.util.DiameterSessionHolder;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.CacheNames;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.exception.TcContextTimeoutException;
import org.qubership.automation.itf.core.util.generator.id.UniqueIdGenerator;
import org.qubership.automation.itf.core.util.manager.ExtensionManager;
import org.qubership.automation.itf.core.util.manager.MonitorManager;
import org.qubership.automation.itf.core.util.pcap.PcapHelper;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.cache.service.impl.PendingDataContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.TCContextCacheService;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.provider.EventBusServiceProvider;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TCContextService {

    public static final ConcurrentHashMap<String, Integer> currentPartitionNumbers = initPartitionNumbers();
    private static final String savedKey = "saved";
    @Getter
    private final TCContextCacheService tcContextCacheService;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final EventBusProvider eventBusProvider;
    private final ProjectSettingsService projectSettingsService;

    private static ConcurrentHashMap<String, Integer> initPartitionNumbers() {
        ConcurrentHashMap<String, Integer> currentPartitionNumbers = new ConcurrentHashMap<>();
        currentPartitionNumbers.put("Default", 1);
        return currentPartitionNumbers;
    }

    public static int getCurrentPartitionNumberByProject(UUID projectUuid) {
        Integer i = currentPartitionNumbers.get(projectUuid.toString());
        return (i == null) ? currentPartitionNumbers.get("Default") : i;
    }

    public void refreshPartitionNumbers(Map<String, Integer> newData) {
        currentPartitionNumbers.putAll(newData);
    }

    public void start(TcContext tcContext) {
        if (!Status.NOT_STARTED.equals(tcContext.getStatus())) {
            return;
        }
        tcContext.setStartTime(new Date());
        tcContext.setStatus(Status.IN_PROGRESS);
        String dumpfilePath = PcapHelper.startTcpDumpCreating(tcContext.getID().toString());
        if (StringUtils.isNotEmpty(dumpfilePath)) {
            tcContext.getReportLinks().put("Download TCPDump", dumpfilePath);
        }
        CacheServices.getTcContextCacheService().set(tcContext, true);
        long stTime = System.currentTimeMillis();
        eventBusProvider.post(new TcContextEvent.Start(tcContext));
        log.info("post to EventBus TcContextEvent.Start - duration {} ms", System.currentTimeMillis() - stTime);
    }

    public void updateInfo(TcContext tcContext) {
        if (tcContext.isFinished()) {
            return;
        }
        CacheServices.getTcContextCacheService().set(tcContext, true);
        eventBusProvider.post(new TcContextEvent.UpdateInfo(tcContext));
        log.debug("Context {} was updated", tcContext.getID());
    }

    public void stop(TcContext tcContext) {
        stop((BigInteger) tcContext.getID(), getTenantId(tcContext));
    }

    public void stop(BigInteger tcContextId, String tenantId) {
        executorToMessageBrokerSender.sendMessageToTcContextOperationsTopic(
                new TcContextOperationMessage(Status.STOPPED.name(), tcContextId), tenantId);
    }

    public void stopOnCurrentServiceInstance(TcContext tcContext) {
        if (Status.IN_PROGRESS.equals(tcContext.getStatus())
                || Status.PAUSED.equals(tcContext.getStatus())
                || Status.NOT_STARTED.equals(tcContext.getStatus())) {
            tcContext.setEndTime(new Date());
            tcContext.setStatus(Status.STOPPED);
            performExtraFinishActions(tcContext);
            eventBusProvider.post(new TcContextEvent.Stop(tcContext));
        }
    }

    public void resumeOnCurrentServiceInstance(TcContext tcContext) {
        if (!Status.PAUSED.equals(tcContext.getStatus())) {
            return;
        }
        tcContext.setStatus(Status.IN_PROGRESS);
        SituationExtension situationExtension = ExtensionManager.getInstance()
                .getExtension(tcContext, SituationExtension.class);
        if (situationExtension.getSituationInstanceIds().size() > 1) {
            tcContext.setStatus(Status.FAILED);
            updateInfo(tcContext);
            String error = "There are more than one situation instance for resuming. Resuming can not be "
                    + "continued, tcContext will be failed.";
            IllegalArgumentException exception = new IllegalArgumentException(error);
            log.error("Error occurred while resuming the {} context: ", tcContext.getName(), exception);
            throw exception;
        }
        CacheServices.getTcBindingCacheService().bind(tcContext);
        updateInfo(tcContext);
        ExecutionServices.getExecutionProcessManagerService().resume(tcContext);
    }

    public void prolong(TcContext tcContext) {
        Status tcContextStatus = tcContext.getStatus();
        if (Status.FAILED.equals(tcContextStatus) || Status.STOPPED.equals(tcContextStatus)) {
            tcContext.setStatus(Status.IN_PROGRESS);
            eventBusProvider.post(new TcContextEvent.Continue(tcContext));
        }
    }

    public void finish(TcContext tcContext) {
        if (Status.IN_PROGRESS.equals(tcContext.getStatus()) || Status.PAUSED.equals(tcContext.getStatus())) {
            finalizeContext(tcContext, (tcContext.isValidationFailed()) ? Status.FAILED : Status.PASSED);
            performExtraFinishActions(tcContext);
        }
        if (!tcContext.isFinishEventSent()) {
            synchronized (tcContext) {
                if (!tcContext.isFinishEventSent()) {
                    eventBusProvider.post(new TcContextEvent.Finish(tcContext));
                    tcContext.setFinishEventAsSent();
                }
            }
        }
    }

    public void fail(TcContext tcContext) {
        fail(tcContext, false);
    }

    private void fail(TcContext tcContext, boolean byTimeout) {
        Status contextStatus = tcContext.getStatus();
        if (Status.NOT_STARTED.equals(contextStatus)
                || Status.IN_PROGRESS.equals(contextStatus) || Status.FAILED.equals(contextStatus)) {
            finalizeContext(tcContext, byTimeout
                    ? Status.FAILED_BY_TIMEOUT
                    : Status.FAILED);
            performExtraFinishActions(tcContext);
        }
        if (!tcContext.isFailEventSent()) {
            synchronized (tcContext) {
                if (!tcContext.isFailEventSent()) {
                    eventBusProvider.post(new TcContextEvent.Fail(tcContext));
                    tcContext.setFailEventAsSent();
                }
            }
        }
    }

    public void failByTimeout(TcContext tcContext) {
        fail(tcContext, true);
    }

    public void terminateByTimeout(TcContext tcContext) {
        if (tcContext.isRunning() || Status.PAUSED.equals(tcContext.getStatus())) {
            // If we are here, there is no error in the context yet. So, finish it as Passed, if it's initiated by
            // inbound situation.
            if (tcContext.getInitiator() instanceof SituationInstance) {
                finish(tcContext);
                log.debug("TC context (inbound) is terminated by timeout (old status: {})", tcContext.getStatus());
            } else {
                String message = String.format("TC context [%s] '%s' is failed by timeout (old status: %s)",
                        tcContext.getID(), tcContext.getName(), tcContext.getStatus());
                log.error(message);
                terminateCallChainContextByTimeout(tcContext.getID(), message);
            }
        } else {
            log.warn("TC context [{}] '{}' is removed from {} cache due to expired, but NOT failed by timeout due "
                            + "to conditions: status {}", tcContext.getID(), tcContext.getName(),
                    CacheNames.ATP_ITF_TC_CONTEXTS, tcContext.getStatus());
        }
    }

    private void terminateCallChainContextByTimeout(Object tcId, String message) {
        SubscriberData subscriberData = NextCallChainEventSubscriberHolder.getInstance().getSubscriberData(tcId);
        if (subscriberData != null) {
            EventBusServiceProvider.getStaticReference()
                    .post(new NextCallChainEvent.FailByTimeout(subscriberData.getSubscriberId(),
                            new TcContextTimeoutException(message)));
        }
    }

    public void disableStepByStepOnCurrentServiceInstance(BigInteger contextId) {
        SubscriberData subscriberData = NextCallChainEventSubscriberHolder.getInstance().getSubscriberData(contextId);
        if (subscriberData != null) {
            SituationInstance situationInstance = DefferedSituationInstanceHolder.getInstance().get(contextId);
            if (situationInstance != null) {
                TcContext context = situationInstance.getContext().getTC();
                if (context != null) {
                    context.setRunStepByStep(false);
                    tcContextCacheService.set(context, true);
                } else {
                    log.error("Error while trying to turn off step-by-step mode for tcContext [{}]: "
                                    + "There is no tcContext linked with deferred SituationInstance {}!", contextId,
                            situationInstance);
                }
            } else {
                log.error("Error while trying to turn off step-by-step mode for tcContext [{}]: "
                        + "There is no SituationInstance in the Holder!", contextId);
            }
        }
    }

    public void pause(TcContext tcContext) {
        executorToMessageBrokerSender.sendMessageToTcContextOperationsTopic(
                new TcContextOperationMessage(Status.PAUSED.name(), (BigInteger) tcContext.getID()),
                getTenantId(tcContext));
    }

    public void pauseOnCurrentServiceInstance(TcContext tcContext) {
        if (Status.IN_PROGRESS.equals(tcContext.getStatus()) || tcContext.isRunStepByStep()) {
            SubscriberData subscriberData = NextCallChainEventSubscriberHolder.getInstance()
                    .getSubscriberData(tcContext.getID());
            if (subscriberData == null) {
                String error = "There is no subscriber to process 'pause' event.";
                IllegalArgumentException exception = new IllegalArgumentException(error);
                log.error("Error occurred while pausing the {} context: ", tcContext.getName(), exception);
                throw exception;
            }
            tcContext.setStatus(Status.PAUSED);
            CacheServices.getTcBindingCacheService().bind(tcContext); // TODO check - do we really need it?
            CacheServices.getTcContextCacheService().set(tcContext, true);
            eventBusProvider.post(new TcContextEvent.Pause(tcContext));
            NextCallChainEvent pauseEvent = new NextCallChainEvent.Pause(subscriberData.getParentSubscriberId(),
                    (CallChainInstance) tcContext.getInitiator());
            pauseEvent.setID(subscriberData.getSubscriberId());
            eventBusProvider.post(pauseEvent);
        }
    }

    public void updateContext(TcContext tcContext) {
        setOrMergeTc(tcContext);
    }

    public void setMessageParameters(TcContext tcContext, Map<String, MessageParameter> messageParameters) {
        setMessageParameters(tcContext, messageParameters.values());
    }

    public void setMessageParameters(TcContext tcContext, Collection<MessageParameter> messageParameters) {
        if (!tcContext.containsKey(savedKey)) {
            tcContext.create(savedKey);
        }
        for (MessageParameter parameter : messageParameters) {
            if (parameter.isAutosave()) {
                tcContext.put(String.format("%s.%s", savedKey, parameter.getParamName()), (parameter.isMultiple())
                        ? parameter.getMultipleValue()
                        : parameter.getSingleValue());
            }
        }
    }

    @Deprecated
    public int getDurationMinutes(TcContext tcContext) {
        Date endTime = tcContext.getEndTime();
        Date startTime = tcContext.getStartTime();
        if (startTime == null) {
            return 0;
        } else if (endTime == null) {
            return (int) ((System.currentTimeMillis() - startTime.getTime()) / (1000 * 60) + 1);
        } else {
            return (int) ((endTime.getTime() - startTime.getTime()) / (1000 * 60) + 1);
        }
    }

    /**
     * This notification is invoked during TcContext events processing in the LoggerSubscriber
     * Events are TcContextEvent.Fail, TcContextEvent.Finish, TcContextEvent.Stop
     */
    public void notifyATP(TcContext tcContext) {
        if (tcContext.getStartedByAtp()) {
            Object object = MonitorManager.getInstance().get(tcContext.getID().toString());
            synchronized (object) {
                object.notify();
            }
        }
    }

    public void mergePendingContextsIfAny(TcContext tcContext) {
        PendingDataContextsCacheService pendingDataContextsCacheService = CacheServices
                .getPendingDataContextsCacheService();
        Map<Object, TcContext> pendingDataContext = pendingDataContextsCacheService.getContextById(tcContext.getID());
        if (pendingDataContext != null && !pendingDataContext.isEmpty()) {
            putPendingData(tcContext, pendingDataContext);
            pendingDataContextsCacheService.clearPendingDataContext(tcContext.getID());
        }
    }

    private void putPendingData(TcContext tcContext, Map<Object, TcContext> pendingContextData) {
        Set<Map.Entry<Object, TcContext>> entries = pendingContextData.entrySet();
        for (Map.Entry<Object, TcContext> entry : entries) {
            TcContext pendingContext = entry.getValue();
            tcContext.putIfAbsent(pendingContext);
        }
    }

    private void setOrMergeTc(TcContext tcContext) {
        AbstractContainerInstance initiator = tcContext.getInitiator();
        if (initiator.getContext().tc() == null) {
            initiator.getContext().setTC(tcContext);
        } else {
            initiator.getContext().tc().merge(tcContext);
        }
    }

    private void finalizeContext(TcContext tcContext, Status status) {
        tcContext.setEndTime(new Date());
        tcContext.setStatus(status);
        AbstractContainerInstance initiator = tcContext.getInitiator();
        if (initiator != null) {
            initiator.setStatus(status);
        }
    }

    private void performExtraFinishActions(TcContext tcContext) {
        PcapHelper.stopTcpDumpCreating(tcContext.getID().toString());
        CacheServices.getTcBindingCacheService().unbind(tcContext);
        NextCallChainEventSubscriberHolder.getInstance().remove(tcContext.getID());
        DiameterSessionHolder.getInstance().remove(tcContext.getID());
        List<NextCallChainSubscriber> subscribers = CacheServices.getCallchainSubscriberCacheService()
                .unregisterAllSubscribers(tcContext.getID());
        if (subscribers != null) {
            for (NextCallChainSubscriber subscriber : subscribers) {
                eventBusProvider.unregister(subscriber);
            }
        }
    }

    public void updateLastAccess(TcContext tcContext) {
        CacheServices.getTcContextCacheService().set(tcContext, true);
        tcContext.setLastUpdateTime(System.currentTimeMillis());
        /*
            Get the object from MonitorManager cache in order to restart expiration control (set via expireAfterAccess)
        */
        if (tcContext.getStartedByAtp()) {
            MonitorManager.getInstance().get(tcContext.getID().toString());
        }
    }

    public TcContext createInMemory(BigInteger projectId, UUID projectUuid) {
        return createInMemory(UniqueIdGenerator.generate(), projectId, projectUuid);
    }

    @NotNull
    private TcContext createInMemory(Object id, BigInteger projectId, UUID projectUuid) {
        TcContext context = new TcContext();
        context.setID(id);
        context.setProjectId(projectId);
        context.setProjectUuid(projectUuid);
        context.setPodName(Config.getConfig().getRunningHostname());
        return context;
    }

    @NotNull
    private String getTenantId(TcContext tcContext) {
        try {
            return tcContext.getInitiator().getContext().getProjectUuid().toString();
        } catch (Exception e) {
            log.error("Can't get tenant id from TCContext. TCContext id: {}", tcContext.getID());
            return "";
        }
    }
}
