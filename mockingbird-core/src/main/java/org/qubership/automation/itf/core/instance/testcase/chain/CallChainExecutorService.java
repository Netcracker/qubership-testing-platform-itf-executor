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

package org.qubership.automation.itf.core.instance.testcase.chain;

import java.math.BigInteger;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.execution.ExecutorServiceProviderFactory;
import org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber;
import org.qubership.automation.itf.core.metric.MetricsAggregateService;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.event.CallChainEvent;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.testcase.TestCase;
import org.qubership.automation.itf.core.report.ReportLinkExtension;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.engine.EngineBeforeIntegration;
import org.qubership.automation.itf.core.util.engine.EngineIntegration;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.generator.id.UniqueIdGenerator;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.ExtensionManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CallChainExecutorService {

    private final ReportLinkCollector reportLinkCollector;
    private final EventBusProvider eventBusProvider;
    private final MetricsAggregateService metricsAggregateService;
    /*
        No one BeforeIntegrations currently.
        It's too expensive to invoke this mechanics any time to 100% zero result.
        Remove this setting as a part of future implementation (if any).
     */
    private final boolean isBeforeIntegrationsOff = true;
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    public void runBeforeIntegrations(CallChainInstance instance) {
        if (isBeforeIntegrationsOff) {
            return;
        }
        try {
            Set<IntegrationConfig> confs = CoreObjectManager.getInstance().getManager(StubProject.class)
                    .getById(instance.getContext().tc().getProjectId()).getIntegrationConfs();
            if (confs.isEmpty()) {
                return;
            }
            for (String toolName : EngineIntegrationRegistry.getInstance().getAvailableIntegrations()) {
                EngineIntegration integration = EngineIntegrationRegistry.getInstance().find(toolName);
                if (integration instanceof EngineBeforeIntegration) {
                    for (IntegrationConfig config : confs) {
                        if (config.getTypeName().equals(toolName)) {
                            ((EngineBeforeIntegration) integration).executeBefore(instance, config);
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            log.error("Starting context {}; error while 'before-integrations' running: {}",
                    instance.getContext().tc().getID(), ex.getMessage());
        }
    }

    private void _executeInstance(CallChainInstance instance) {
        TcContext tc = instance.getContext().tc();
        instance.setStatus(Status.IN_PROGRESS);
        instance.setStartTime(new Date());
        ExecutionServices.getTCContextService().start(tc);
        log.info("Executing Call Chain {}...", instance);
        runBeforeIntegrations(instance);
        eventBusProvider.post(new CallChainEvent.Start(instance));
        NextCallChainEvent event = new NextCallChainEvent(null, instance);
        NextCallChainSubscriber subscriber = new NextCallChainSubscriber(event);
        subscriber.registerSubscriberInHolder();
        eventBusProvider.register(subscriber, EventBusProvider.Priority.HIGH);
        eventBusProvider.post(event);
    }

    /*
     *   This 'prepare' method is to prepare NESTED steps ONLY
     */
    public CallChainInstance prepare(
            @Nonnull BigInteger projectId,
            @Nonnull UUID projectUuid,
            @Nonnull TestCase testCase,
            @Nullable TcContext context,
            @Nonnull Environment environment, @Nullable IDataSet dataSet, @Nullable JsonContext customDataset)
            throws Exception {
        return prepare(projectId, projectUuid, testCase, context, environment, dataSet, customDataset,
                false, false, true);
    }

    public CallChainInstance prepare(
            @Nonnull BigInteger projectId,
            @Nonnull UUID projectUuid,
            @Nonnull TestCase testCase,
            @Nullable TcContext context,
            @Nonnull Environment environment,
            @Nullable IDataSet dataSet, @Nullable JsonContext customDataset, boolean runValidation) throws Exception {
        return prepare(projectId, projectUuid, testCase, context, environment, dataSet, customDataset,
                true, runValidation, false);
    }

    public CallChainInstance prepare(
            @Nonnull BigInteger projectId,
            @Nonnull UUID projectUuid,
            @Nonnull TestCase testCase,
            @Nullable TcContext context,
            @Nonnull Environment environment,
            @Nullable IDataSet dataSet,
            @Nullable JsonContext customDataset, boolean startedByAtp, boolean runValidation) throws Exception {
        return prepare(projectId, projectUuid, testCase, context, environment, dataSet, customDataset,
                startedByAtp, runValidation, false);
    }

    public CallChainInstance prepare(
            @Nonnull BigInteger projectId,
            @Nonnull UUID projectUuid,
            @Nonnull TestCase testCase,
            @Nullable TcContext context,
            @Nonnull Environment environment,
            @Nullable IDataSet dataSet,
            @Nullable JsonContext customDataset, boolean startedByAtp, boolean runValidation, boolean isNestedStep)
            throws Exception {
        MdcUtils.put(MdcField.CALL_CHAIN_ID.toString(), testCase.getID().toString());
        log.info("Preparing instance for call chain {}...", testCase);
        CallChainInstance instance = new CallChainInstance();
        instance.setID(UniqueIdGenerator.generate());
        instance.setParent(null);
        instance.setStepContainer(testCase);
        instance.setName(testCase.getName());
        prepareContext(projectId, projectUuid, instance, context, dataSet, customDataset, environment, startedByAtp,
                runValidation);
        prepareKeys(instance);
        return instance;
    }

    /**
     * Enclosed into try-catch block with just logging exception (if any),
     * because we do NOT want to break normal execution in case of lazy- or some other exceptions here.
     */
    public void refreshExtensionLinks(CallChainInstance instance) {
        try {
            Map<String, String> links = reportLinkCollector.collect(instance.getContext().tc());
            ReportLinkExtension extension = ExtensionManager.getInstance()
                    .getExtension(instance.getContext().tc(), ReportLinkExtension.class);
            extension.getLinks().putAll(links);
            for (Map.Entry<String, String> entry : links.entrySet()) {
                log.info("Report link for call chain {}. {} : {}", instance, entry.getKey(), entry.getValue());
            }
        } catch (Throwable throwable) {
            log.error("Error while collecting of report links", throwable);
        }
    }

    @SuppressWarnings("Duplications")
    private TcContext prepareContext(BigInteger projectId, UUID projectUuid, CallChainInstance instance,
                                     TcContext context, IDataSet dataSet, JsonContext customDataset,
                                     Environment env, boolean startedByAtp, boolean runValidation) {
        if (context == null) {
            context = createTcContext(projectId, projectUuid, instance, dataSet, env, startedByAtp, runValidation);
        } else {
            if (context.getProjectId() == null) {
                context.setProjectId(projectId);
                context.setProjectUuid(projectUuid);
            }
        }
        instance.getContext().setProjectId(projectId);
        instance.getContext().setProjectUuid(projectUuid);
        instance.getContext().setTC(context);
        if (customDataset != null) {
            if (dataSet != null) {
                /* Duplicated parameters in the dataset and customDataset will be removed from custom in dataSet.read
                (customDataset); */
                context.merge(dataSet.read(customDataset, context.getProjectId()));
                instance.setDatasetName(dataSet.getName());
            }
            context.merge(customDataset);
        } else {
            if (dataSet != null) {
                context.merge(dataSet.read(context.getProjectId()));
                instance.setDatasetName(dataSet.getName());
            }
        }
        return context;
    }

    private void prepareKeys(CallChainInstance instance) {
        TemplateEngine engine = TemplateEngineFactory.get();
        InstanceContext context = instance.getContext();
        String parsedKey;
        for (String key : ((CallChain) instance.getStepContainer()).getKeys()) {
            parsedKey = engine.process(instance.getStepContainer(), key, context, "CallChain Key '" + key + "'");
            if (StringUtils.isBlank(parsedKey)) {
                continue;
            }
            // We must not ADD key but BIND tcContext with it - only if key value is new
            if (CacheServices.getTcBindingCacheService().bind(parsedKey, context.tc())) {
                log.info("Context key for instance {} is {}", instance, parsedKey);
            }
        }
    }

    public CallChainInstance executeInstance(final CallChainInstance instance) {
        metricsAggregateService.incrementCallChainCountToProject(instance.getContext().getTC().getProjectUuid(),
                instance.getName());
        ExecutorServiceProviderFactory.get().requestForRegular().submit(
                () -> execute(instance, "Error executing in separate thread {}"));
        return instance;
    }

    @SuppressWarnings("Duplicates")
    public void executeInstance(final CallChainInstance instance, boolean waitForFulfillment) {
        final String error = "Error executing in separate thread instance id {}";
        metricsAggregateService.incrementCallChainCountToProject(instance.getContext().getTC().getProjectUuid(),
                instance.getName());
        ExecutorServiceProviderFactory.get().requestForRegular()
                .submit(createRunnableWithCurrentSecurityContext(instance, error));
        if (waitForFulfillment) {
            waitStatusNotInProgress(instance);
        }
    }

    public void reportErrorThenStop(final CallChainInstance instance,
                                    TcContext tcContext,
                                    String title,
                                    String errorTitle,
                                    String errorMessage,
                                    Throwable t) {
        if (!tcContext.isFinished()) {
            if (tcContext.isNeedToReportToAtp()) {
                Report.openSection(instance, title);
                Report.error(instance, errorTitle, errorMessage, t);
                Report.closeSection(instance);
                Report.stopRun(instance.getContext(), Status.FAILED);
            }
            ExecutionServices.getTCContextService().stopOnCurrentServiceInstance(tcContext);
        }
    }

    private void execute(final CallChainInstance instance, String errorMessage) {
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(String.valueOf(instance.getContext().getProjectUuid()));
        }
        String oldThreadName = cutTail(Thread.currentThread().getName(), " [");
        OffsetDateTime started = OffsetDateTime.now();
        TcContext tcContext = instance.getContext().tc();
        try {
            String curName = oldThreadName + " [" + tcContext.getID() + "] " + instance.getName();
            Thread.currentThread().setName(curName);
            TxExecutor.execute((Callable<Void>) () -> {
                fillMdsFields(instance);
                _executeInstance(instance);
                /*
                    Do NOT delete the following command. It's like flush().
                    Performance is greatly improved.
                 */
                log.info("Thread {}: instance is executed", curName);
                return null;
            }, TxExecutor.readOnlyTransaction());
        } catch (Throwable t) {
            log.error(errorMessage, instance, t);
            reportErrorThenStop(instance, tcContext, "Errors while callchain startup or execution",
                    t.getMessage(), t.getMessage(), t);
        } finally {
            if (!tcContext.isNotified()) {
                ExecutionServices.getTCContextService().notifyATP(tcContext);
            }
            Thread.currentThread().setName(oldThreadName);
            Duration durationBetween = Duration.between(started, OffsetDateTime.now());
            metricsAggregateService.recordExecuteCallchainDuration(tcContext.getProjectUuid(),
                    instance.getName(), durationBetween);
            MDC.clear();
        }
    }

    private void fillMdsFields(CallChainInstance instance) {
        try {
            TcContext tc = instance.getContext().tc();
            MdcUtils.put(MdcField.CONTEXT_ID.toString(), tc.getID().toString());
            MdcUtils.put(MdcField.PROJECT_ID.toString(), instance.getContext().getProjectUuid());
            MdcUtils.put(MdcField.CALL_CHAIN_ID.toString(), instance.getTestCaseId().toString());
            String testRunId = tc.get("testRunId", String.class);
            String executionRequestId = tc.get("executionRequestId", String.class);
            if (StringUtils.isNotBlank(testRunId) && StringUtils.isNotBlank(executionRequestId)) {
                MdcUtils.put(MdcField.TEST_RUN_ID.toString(), testRunId);
                MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), executionRequestId);
            }
        } catch (Exception e) {
            log.error("Can't fill MDC fields", e);
        }
    }

    private String cutTail(String str, String prefix) {
        int i = str.indexOf(prefix);
        return (i == -1) ? str : str.substring(0, i);
    }

    private void waitStatusNotInProgress(CallChainInstance instance) {
        Status status = instance.getStatus();
        int i = 0; // Maximum wait is 50 seconds. This method is invoked for SVT only.
        while ((Status.IN_PROGRESS.equals(status) || Status.NOT_STARTED.equals(status)) && i < 50) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Callchain startup waiting is interrupted", e);
            }
            i++;
            status = instance.getStatus();
        }
    }

    private Runnable createRunnableWithCurrentSecurityContext(final CallChainInstance instance, final String error) {
        Runnable originalRunnable = () -> execute(instance, error);
        SecurityContext context = SecurityContextHolder.getContext();
        return new DelegatingSecurityContextRunnable(originalRunnable, context);
    }

    private TcContext createTcContext(BigInteger projectId, UUID projectUuid, CallChainInstance instance,
                                      IDataSet dataSet, Environment environment, boolean startedByAtp,
                                      boolean runValidation) {
        TcContext context = ExecutionServices.getTCContextService().createInMemory(projectId, projectUuid);
        context.setStartedByAtp(startedByAtp);
        context.setNeedToReportToAtp(startedByAtp);
        context.setStartValidation(runValidation);
        context.setInitiator(instance);
        context.setName(String.format("%s [%s]", instance.getName(),
                dataSet == null ? "No Data Set" : dataSet.getName()));
        context.setEnvironmentId((BigInteger) environment.getID());
        context.setEnvironmentName(environment.getName());
        context.setProjectId(projectId);
        context.setProjectUuid(projectUuid);
        context.setTimeToLive();
        return context;
    }
}
