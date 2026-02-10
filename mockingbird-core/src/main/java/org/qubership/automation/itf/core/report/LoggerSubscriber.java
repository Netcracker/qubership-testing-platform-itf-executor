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

package org.qubership.automation.itf.core.report;

import java.util.Date;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ContextManager;
import org.qubership.automation.itf.core.model.event.CallChainEvent;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.event.StepEvent;
import org.qubership.automation.itf.core.model.event.TcContextEvent;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.report.producer.ReportWorker;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.MonitorManager;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Component
public class LoggerSubscriber {

    public static final Logger LOGGER = LoggerFactory.getLogger(LoggerSubscriber.class);
    public static final String CLIENT_IP = "clientIP";
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;
    private final ReportWorker worker;
    private final RunSubscriberInterface runSubscriber;
    private final ReportLinkCollector reportLinkCollector;
    private final EventBusProvider eventBusProvider;

    @Autowired
    public LoggerSubscriber(ReportWorker worker, RunSubscriberInterface runSubscriber,
                            ReportLinkCollector reportLinkCollector,
                            EventBusProvider eventBusProvider) {
        this.worker = worker;
        this.runSubscriber = runSubscriber;
        this.reportLinkCollector = reportLinkCollector;
        this.eventBusProvider = eventBusProvider;
    }

    @PostConstruct
    public void init() {
        eventBusProvider.register(this, EventBusProvider.Priority.HIGH);
    }

    @PreDestroy
    public void destroy() {
        eventBusProvider.unregister(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onContextStart(TcContextEvent.Start event) {
        String projectUuid = event.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        fillReportLinks(event.getContext(), false);
        if (event.getContext().isNeedToReportToAtp()) {
            Report.startRun(event.getContext());
        }
        if (event.getContext().isNeedToReportToItf()) {
            worker.submit(event.getContext(), event.getDate(), event.getContext().getProjectId(), projectUuid);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onContextUpdateInfo(TcContextEvent.UpdateInfo event) {
        String projectUuid = event.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        if (event.getContext().isNeedToReportToItf()) {
            worker.submit(event.getContext(), event.getDate(), event.getContext().getProjectId(), projectUuid);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onContextFail(TcContextEvent.Fail event) {
        String projectUuid = event.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        logContextEvent(event, Status.FAILED, projectUuid);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onContextFinish(TcContextEvent.Finish event) {
        String projectUuid = event.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        logContextEvent(event, Status.PASSED, projectUuid);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onContextStop(TcContextEvent.Stop event) {
        String projectUuid = event.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        logContextEvent(event, Status.STOPPED, projectUuid);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onContextPaused(TcContextEvent.Pause event) {
        String projectUuid = event.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        if (event.getContext().isNeedToReportToItf()) {
            worker.submit(event.getContext(), event.getDate(), event.getContext().getProjectId(), projectUuid);
        }
    }

    //region Chain Start/Finish
    @Subscribe
    @AllowConcurrentEvents
    public void onChainStart(CallChainEvent.Start event) {
        // For Start event, do NOT send CallChainInstance to ITF reporting.
        // If it's initiator, it will be attached to TcContext message.
        CallChainInstance chainInstance = event.getInstance();
        if (chainInstance.getContext().tc().isNeedToReportToAtp()) {
            Report.openSection(chainInstance, "Call chain [" + chainInstance.getStepContainer().getName() + "]");
            if (chainInstance.stepsIsDisabled()) {
                Report.warn(chainInstance,
                        "Steps in Call Chain [" + chainInstance.getStepContainer().getName() + "] " + "are disabled",
                        "All steps are disabled");
            } else {
                Report.reportCallChainInfo(chainInstance);
            }
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onChainFinish(CallChainEvent.Finish event) {
        CallChainInstance chainInstance = submitCallChainInstance(event.getInstance(), "Finish chain ",
                event.getDate());
        if (chainInstance.getContext().tc().isNeedToReportToAtp() && chainInstance != chainInstance.getContext().getTC().getInitiator()) {
            Report.closeSection(chainInstance);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onChainTerminate(CallChainEvent.Terminate event) {
        CallChainInstance chainInstance = submitCallChainInstance(event.getInstance(), "Terminate chain ",
                event.getDate());
        if (chainInstance.getContext().tc().isNeedToReportToAtp()) {
            Report.terminated(chainInstance,
                    "Call Chain [" + chainInstance.getStepContainer().getName() + "] " + "Terminated",
                    "Execution of chain terminated", chainInstance.getError());
        }
    }

    //region Situation Start/Finish/Terminate
    @Subscribe
    @AllowConcurrentEvents
    public void onSituationStart(SituationEvent.Start event) {
        /* Do NOT report start of Situation into itf-reporting.
            And, it's never reported into RAM2 (please check RAM2ReportAdapter code).
            So, it looks like this handler can be deleted.
        */
        /*
        SituationInstance situationInstance = submitSituationInstance(event.getSituationInstance(), "Start
        situation", event.getDate());
        if (situationInstance.getContext().tc().isNeedToReportToAtp()) {
            Report.openSection(situationInstance, ((event.getSource() instanceof CallChain)
                    ? "CallChain "
                    : "") + "Situation [" + situationInstance.getName() + "]");
        }
         */
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onSituationFinish(SituationEvent.Finish event) {
        SituationInstance situationInstance = submitSituationInstance(event.getSituationInstance(), "Finish situation" +
                " ", event.getDate());
        if (situationInstance.getContext().tc().isNeedToReportToAtp()) {
            Report.closeSection(situationInstance);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onSituationTerminate(SituationEvent.Terminate event) {
        SituationInstance situationInstance = submitSituationInstance(event.getSituationInstance(), "Terminate " +
                "situation ", event.getDate());
        if (situationInstance.getContext().tc().isNeedToReportToAtp()) {
            Report.terminated(situationInstance, "Situation [" + situationInstance.getName() + "] Terminated",
                    "Execution situation terminated", situationInstance.getError());
            Report.closeSection(situationInstance);
        }
    }
    //endregion
    //region Step Start/Finish/Terminate

    @Subscribe
    @AllowConcurrentEvents
    public void onStepFinish(StepEvent.Finish event) {
        StepInstance stepInstance = submitStepInstance(event.getStepInstance(), "Finish step ", event.getDate());
        logStepInfo(stepInstance, "%s");
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onStepSkip(StepEvent.Skip event) {
        StepInstance stepInstance = submitStepInstance(event.getStepInstance(), "Skip step ", event.getDate());
        logStepInfo(stepInstance, "[%s] Skipped");
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onStepTerminate(StepEvent.Terminate event) {
        StepInstance stepInstance = submitStepInstance(event.getStepInstance(), "Terminate step ", event.getDate());
        if (stepInstance.getContext().tc().isNeedToReportToAtp() && !(stepInstance.getContext().tc().getStartedFrom()
                .equals(StartedFrom.RAM2))) {
            Report.error(stepInstance, "[" + stepInstance.getStep().getName() + "] Terminated",
                    stepInstance.getContext().sp(), stepInstance.getError());
            Report.closeSection(stepInstance.getParent());
        }
    }

    /*
     *   Common method used for logging of TcContextEvent.Finish, TcContextEvent.Fail and TcContextEvent.Stop events.
     *   The corresponding statuses are: Status.PASSED, Status.FAILED and Status.STOPPED.
     *   Each individual action in the method is enclosed into try-catch block
     *       to ensure that an exception in one action will NOT prevent other actions to be performed.
     */
    private void logContextEvent(TcContextEvent event, Status status, String tenantId) {
        TcContext tcContext = event.getContext();
        if (!(Status.STOPPED.equals(status)
                || Status.FAILED_BY_TIMEOUT.equals(tcContext.getStatus()))) {
            try {
                ReportIntegration.getInstance().runAndReportAfterIntegrations(tcContext);
            } catch (Throwable ex) {
                LOGGER.error("Context {} is {}; then integration error is thrown: {}",
                        tcContext.getID(), status.toString(), ex.getMessage());
            }
        }
        try {
            if (tcContext.isNeedToReportToItf()) {
                worker.submit(tcContext, event.getDate(), tcContext.getProjectId(), tenantId);
            }
        } catch (Throwable ex) {
            LOGGER.error("Context {} is {}; then inner reporting error is thrown: {}",
                    tcContext.getID(), status.toString(), ex.getMessage());
        }
        ExecutionServices.getTCContextService().notifyATP(tcContext);
        if (tcContext.isNeedToReportToAtp()) {
            LOGGER.debug("Context is {}: {}", status.toString(), event.getContext());
            Report.stopRun(InstanceContext.from(tcContext, new SpContext()), status);
        }
        if (!(tcContext.getStartedFrom().equals(StartedFrom.RAM2))) {
            CacheServices.getTcContextCacheService().evict(tcContext);
        }
    }

    private void logStepInfo(StepInstance stepInstance, String format) {
        if (stepInstance.getContext().tc().isNeedToReportToAtp()) {
            Report.info(stepInstance, String.format(format, stepInstance.getStep().getName()),
                    stepInstance.getContext().sp());
        }
    }
    //endregion

    private void sendNotification(String message, TcContext tcContext) {
        Object object = MonitorManager.getInstance().get(tcContext.getID().toString());
        synchronized (object) {
            try {
                object.wait();
                TcContext context = CoreObjectManager.getInstance()
                        .getSpecialManager(TcContext.class, ContextManager.class).getById(tcContext.getID());
                if (context.containsKey(CLIENT_IP)) {
                    runSubscriber.send(message, context.get(CLIENT_IP).toString());
                }
            } catch (InterruptedException e) {
                LOGGER.error("Failed while waiting notification", e);
            }
        }
    }

    private void fillReportLinks(TcContext context, boolean standalone) {
        try {
            Map<String, String> reportLinks = context.getReportLinks();
            if (context.isNeedToReportToItf()) {
                reportLinks.put("ITF context link", reportLinkCollector.getLinkToObject(
                        context.getProjectId(), context.getProjectUuid(), context.getID(),
                        "#/context/", standalone));
            }
            AbstractContainerInstance initiator = context.getInitiator();
            if (initiator instanceof SituationInstance) {
                if (((SituationInstance) initiator).getSystemId() != null) {
                    reportLinks.put("System link", reportLinkCollector.getLinkToObject(
                            context.getProjectId(), context.getProjectUuid(),
                            ((SituationInstance) initiator).getSystemId(),
                            "#/system/", standalone));
                }
            } else if (initiator instanceof CallChainInstance) {
                reportLinks.put("Callchain link", reportLinkCollector.getLinkToObject(
                        context.getProjectId(), context.getProjectUuid(),
                        ((CallChainInstance) initiator).getTestCaseId(),
                        "#/callchain/", standalone));
            }
        } catch (Throwable ex) {
            LOGGER.error("Starting context {}; error while collecting of report links: {}",
                    context.getID(), ex.getMessage());
        }
    }

    @Nonnull
    private CallChainInstance submitCallChainInstance(CallChainInstance callChainInstance,
                                                      String logMessage,
                                                      Date eventDate) {
        String projectUuid = callChainInstance.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        LOGGER.debug(logMessage + callChainInstance);
        if (callChainInstance.getContext().tc().isNeedToReportToItf()) {
            worker.submit(callChainInstance, eventDate, callChainInstance.getContext().tc().getProjectId(),
                    projectUuid);
        }
        return callChainInstance;
    }

    @Nonnull
    private StepInstance submitStepInstance(StepInstance stepInstance, String logMessage, Date eventDate) {
        String projectUuid = stepInstance.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        LOGGER.debug(logMessage + stepInstance);
        if (stepInstance.getContext().tc().isNeedToReportToItf()) {
            worker.submit(stepInstance, eventDate, stepInstance.getContext().tc().getProjectId(), projectUuid);
        }
        return stepInstance;
    }

    private SituationInstance submitSituationInstance(SituationInstance situationInstance, String logMessage,
                                                      Date eventDate) {
        String projectUuid = situationInstance.getContext().getProjectUuid().toString();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectUuid);
        }
        LOGGER.debug(logMessage + situationInstance);
        if (situationInstance.getContext().tc().isNeedToReportToItf()) {
            worker.submit(situationInstance, eventDate, situationInstance.getContext().tc().getProjectId(),
                    projectUuid);
        }
        return situationInstance;
    }
}
