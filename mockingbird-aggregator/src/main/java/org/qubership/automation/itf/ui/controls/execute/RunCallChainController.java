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

package org.qubership.automation.itf.ui.controls.execute;

import static java.lang.String.format;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSet;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSetList;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.exceptions.configuration.ConfigurationException;
import org.qubership.automation.itf.core.exceptions.execution.CallChainExecutionException;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByParameterAndProjectIdManager;
import org.qubership.automation.itf.core.model.common.Named;
import org.qubership.automation.itf.core.model.container.StepContainer;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.event.CallChainEvent;
import org.qubership.automation.itf.core.model.event.TcContextEvent;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonStorable;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.Maps2;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.engine.EngineControlIntegration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.core.util.pcap.PcapHelper;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.execution.data.CallchainExecutionData;
import org.qubership.automation.itf.execution.manager.CallChainExecutorManager;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.qubership.automation.itf.integration.reports.ReportsService;
import org.qubership.automation.itf.ui.messages.objects.UIDataSet;
import org.qubership.automation.itf.ui.messages.objects.UIRequestBody;
import org.qubership.automation.itf.ui.messages.objects.UISendRun;
import org.qubership.automation.itf.ui.messages.objects.link.UIHyperLink;
import org.qubership.automation.itf.ui.messages.objects.link.UILink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

@Transactional(readOnly = true)
@RestController
public class RunCallChainController extends ExecutorControllerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunCallChainController.class);
    private final CallChainExecutorManager callChainExecutorManager;
    private final ReportLinkCollector reportLinkCollector;
    private final org.springframework.core.env.Environment env;
    private final EventBusProvider eventBusProvider;
    private final ReportsService reportsService;

    @Autowired
    public RunCallChainController(CallChainExecutorManager callChainExecutorManager,
                                  ReportLinkCollector reportLinkCollector,
                                  org.springframework.core.env.Environment env,
                                  EventBusProvider eventBusProvider,
                                  ReportsService reportsService) {
        this.callChainExecutorManager = callChainExecutorManager;
        this.reportLinkCollector = reportLinkCollector;
        this.env = env;
        this.eventBusProvider = eventBusProvider;
        this.reportsService = reportsService;
    }

    private static UISendRun fillUISendRun(UISendRun uiSendRun,
                                           boolean isStarted,
                                           String name,
                                           Map<String, String> links) {
        uiSendRun.setStarted(isStarted);
        uiSendRun.setName(name);
        uiSendRun.setLinks(buildUILinks(links));
        return uiSendRun;
    }

    private static List<UILink> buildUILinks(Map<String, String> links) {
        return Lists.newArrayList(Collections2.transform(
                links.entrySet(), new Function<Map.Entry<String, String>, UIHyperLink>() {
                    @NotNull
                    @Override
                    public UIHyperLink apply(Map.Entry<String, String> input) {
                        UIHyperLink uiLink = new UIHyperLink("Callchain link".equals(input.getKey()) ? "Call Chain" :
                                input.getKey());
                        uiLink.setUrl(input.getValue());
                        return uiLink;
                    }
                }));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/callchain/run", method = RequestMethod.POST)
    @AuditAction(auditAction = "Run CallChain (name {{#name}}, id {{#id}}) on the environment (name {{#environment}}, "
            + "id {{#environmentId}}), project {{#projectId}}/{{#projectUuid}}")
    public List<UISendRun> runEx(
            @RequestParam(value = "name", defaultValue = "") String name,
            @RequestParam(value = "id", defaultValue = "") String id,
            @RequestParam(value = "environment") String environment,
            @RequestParam(value = "environmentId") String environmentId,
            @RequestParam(value = "tccontext", required = false, defaultValue = "") String tcContext,
            @RequestParam(value = "runBvCase", required = false, defaultValue = "false") boolean runBvCase,
            @RequestBody UIRequestBody requestBody,
            @RequestParam(value = "bvAction", required = false, defaultValue = "") String bvAction,
            @RequestParam(value = "needToLogInAtp", required = false) boolean needToLogInAtp,
            @RequestParam(value = "createTcpDump", required = false, defaultValue = "false") boolean createTcpDump,
            @RequestParam(value = "runValidation", required = false, defaultValue = "false") boolean runValidation,
            @RequestParam(value = "runStepByStep", required = false, defaultValue = "false") boolean runStepByStep,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "standalone", required = false, defaultValue = "false") boolean standalone,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        List<UISendRun> result = new ArrayList<>();
        Map<String, String> tcpDumpParams = new HashMap<>();
        tcpDumpParams.put(PcapHelper.TCPDUMP_FILTER_KEY, requestBody.getParams().get(PcapHelper.TCPDUMP_FILTER_KEY));
        tcpDumpParams.put(PcapHelper.TCPDUMP_NETWORK_INTERFACE_NAME_KEY,
                requestBody.getParams().get(PcapHelper.TCPDUMP_NETWORK_INTERFACE_NAME_KEY));
        tcpDumpParams.put(PcapHelper.TCPDUMP_PACKET_COUNT_KEY,
                requestBody.getParams().get(PcapHelper.TCPDUMP_PACKET_COUNT_KEY));
        for (int i = 0; i < requestBody.getDataSetsInfo().length; i++) {
            String datasetName = requestBody.getDataSetsInfo()[i].getDataSetName();
            String datasetId = requestBody.getDataSetsInfo()[i].getDataSetId();
            try {
                result.add(runCallChain(id, ((datasetName == null || datasetName.equals("null"))
                                ? null
                                : datasetName), ((datasetId == null || datasetId.equals("null"))
                                ? null
                                : datasetId), environment, environmentId, requestBody.getDataSet(), runBvCase, bvAction,
                        needToLogInAtp, createTcpDump
                                ? tcpDumpParams
                                : null, runValidation, runStepByStep, projectId, standalone, projectUuid));
            } catch (Throwable ex) {
                UISendRun uiSendRun = new UISendRun();
                uiSendRun.setStarted(false);
                uiSendRun.setName(
                        "Chain '" + name + "' [id=" + id + "]" + ((datasetName == null || datasetName.equals("null"))
                                ? null
                                : "\nDataset Name: " + datasetName) + ((datasetId == null || datasetId.equals("null"))
                                ? null
                                : "\nDataset Id: " + datasetId));
                uiSendRun.setStatus("Chain is not started due to error(s):\n " + ((ex.getMessage() != null)
                        ? ex.getMessage() + ((ex.getCause() != null)
                        ? "\nCause: " + ex.getCause()
                        : "")
                        : ex.toString()));
                result.add(uiSendRun);
                LOGGER.error(uiSendRun.getName() + "\n - " + uiSendRun.getStatus());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/callchain/run/simple", method = RequestMethod.GET)
    @AuditAction(auditAction = "Run (simple) CallChain id {{#id}} with dataset (name {{#dataset}}, id {{#datasetid}}), "
            + "environment {{#environment}}, bvAction {{#bvAction}} in the project {{#projectId}}/{{#projectUuid}}")
    public UISendRun runSimpleGet(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "dataset", required = false) String dataset,
            @RequestParam(value = "timer") int timer,
            @RequestParam(value = "environment") String environment,
            @RequestParam(value = "datasetid", required = false) String datasetid,
            @RequestParam(value = "storedContextId", required = false) String storedContextId,
            @RequestParam(value = "bvAction", required = false, defaultValue = "") String bvAction,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "standadlone", required = false, defaultValue = "false") boolean standalone,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        return runSimple(id, dataset, environment, timer, null, null, storedContextId, bvAction, projectId,
                standalone, projectUuid);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/callchain/run/simple", method = RequestMethod.POST)
    @AuditAction(auditAction = "Run (simple) CallChain id {{#id}} with dataset (name {{#dataset}}, id {{#datasetid}}), "
            + "environment {{#environment}}, bvAction {{#bvAction}} in the project {{#projectId}}/{{#projectUuid}}")
    public UISendRun runSimple(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "dataset", required = false) String dataset,
            @RequestParam(value = "environment") String environment,
            @RequestParam(value = "timer") int timer,
            @RequestBody(required = false) String body,
            @RequestParam(value = "datasetid", required = false) String datasetId,
            @RequestParam(value = "storedContextId", required = false) String storedContextId,
            @RequestParam(value = "bvAction", required = false, defaultValue = "") String bvAction,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "standalone", required = false, defaultValue = "false") boolean standalone,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        if (StringUtils.isBlank(id)) {
            throw new ConfigurationException("Call Chain ID is null");
        }
        if (Strings.isNullOrEmpty(environment)) {
            throw new ConfigurationException("Environment ID is null");
        }
        JsonContext customDataset = null;
        if (StringUtils.isNotEmpty(storedContextId)) {
            String jsonString = reportsService.getContextVariables(storedContextId, projectUuid);
            customDataset = JsonContext.fromJson(jsonString, JsonStorable.class);
        }
        if (customDataset == null && body != null) {
            customDataset = JsonContext.fromJson(body, JsonStorable.class);
        }
        if (Strings.isNullOrEmpty(dataset) && (customDataset == null || customDataset.isEmpty())) {
            throw new ConfigurationException("Data Set is not specified");
        }
        return startChain(dataset, datasetId, customDataset, environment, StringUtils.EMPTY, id,
                format("Can't find " + "call chain with id [%s].", id), false, (timer <= 0), timer, true, bvAction,
                false, null, false, false, null, standalone, projectUuid);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/datasets/ds", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Datasets under DSL id {{#id}} for CallChain id {{#chainId}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public List<UIDataSet> getDataSets(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "chainId", defaultValue = "0") String chainId,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws IOException {
        CallChain chain = CoreObjectManager.getInstance().getManager(CallChain.class).getById(chainId);
        throwExceptionIfNull(chain, "", id, CallChain.class, "get CallChain by id");
        IntegrationConfig bvConfig = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
        EngineControlIntegration engine =
                (EngineControlIntegration) EngineIntegrationRegistry.getInstance().find(BULK_VALIDATOR_INTEGRATION);
        String dssUiUrl = determineDSSUIUrl();
        for (DataSetList dataSetList : chain.getCompatibleDataSetLists(projectUuid)) {
            if (dataSetList.getID().toString().equals(id)) {
                String[] ids;
                String vaId = "";
                String dslId = "";
                if (dataSetList instanceof RemoteDataSetList) {
                    ids = id.split("_");
                    if (ids.length != 2) {
                        throw new IllegalArgumentException(
                                format(DataSetList.class.getSimpleName() + ": id '%s' is " + "incorrect", id));
                    }
                    vaId = ids[0];
                    dslId = ids[1];
                }
                Set<IDataSet> datasets = dataSetList.getDataSets(projectId);
                List<UIDataSet> result = Lists.newArrayListWithCapacity(datasets.size());
                for (IDataSet dataSet : datasets.stream().sorted(Comparator.comparing(Named::getName))
                        .collect(Collectors.toList())) {
                    UIDataSet ds = new UIDataSet();
                    ds.setName(dataSet.getName());
                    ds.setBvCaseId(chain.getBvCases().get(dataSet.getName()));
                    ds.setDefault(chain.getDatasetId() != null && dataSet.getIdDs().equals(chain.getDatasetId()));

                    if (bvConfig != null && !StringUtils.isEmpty(ds.getBvCaseId())) {
                        ds.setBvCaseExist(engine.isExist(chain, bvConfig,
                                Maps2.map("dataset.name", ds.getName()).build(), projectId));
                    }
                    /* Only for remote DataSet tool: Getting dataset link for UI */
                    if (dataSet instanceof RemoteDataSet) {
                        String dsId = dataSet.getIdDs();
                        ds.setId(dsId);
                        ds.setDsLink(dssUiUrl + concatWithSlashIfNeeded(dssUiUrl,
                                String.format(CATALOGUE_LINK_PATTERN_FOR_DS_TOOL, vaId, dslId)));
                    }
                    result.add(ds);
                }
                return result;
            }
        }
        throw new ObjectNotFoundException(DataSetList.class.getSimpleName(), id, null, null);
    }

    private String determineDSSUIUrl() {
        String catalogueUrl = env.getProperty("atp.catalogue.url");
        return StringUtils.isBlank(catalogueUrl) ? StringUtils.EMPTY : catalogueUrl;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/callchain/run/terminateContext", method = RequestMethod.GET)
    @AuditAction(auditAction = "Terminate context with id {{#contextId}} in the project {{#projectUuid}}")
    public void terminateContext(@RequestParam(value = "contextId") BigInteger contextId,
                                 @RequestParam(value = "projectUuid") UUID projectUuid,
                                 @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        ExecutionServices.getTCContextService().stop(contextId, tenantId);
    }

    public UISendRun runCallChain(String id,
                                  String dataset,
                                  String datasetId,
                                  String environment,
                                  String environmentId,
                                  UIDataSet customizedDataset,
                                  boolean runBvCase,
                                  String bvAction,
                                  boolean needToLogInATP,
                                  Map<String, String> tcpDumpParams,
                                  boolean runValidation,
                                  boolean runStepByStep,
                                  BigInteger projectId,
                                  boolean standalone,
                                  UUID projectUuid) throws Exception {
        JsonContext customDataset = toJSONContext(customizedDataset);
        if (StringUtils.isNotBlank(id)) {
            return startChain(dataset, datasetId, customDataset, environment, environmentId, id,
                    format("Can't find call chain with id [%s].", id), runBvCase, false, 40, false, bvAction,
                    needToLogInATP, tcpDumpParams, runValidation, runStepByStep, projectId, standalone, projectUuid);
        } else {
            UISendRun uiSendRun = new UISendRun();
            uiSendRun.setStatus("Chain id is null; chain start is cancelled");
            return uiSendRun;
        }
    }

    private UISendRun startChain(final String dataset,
                                 final String datasetId,
                                 final JsonContext customDataset,
                                 final String environment,
                                 final String environmentId,
                                 final String callChainId,
                                 final String format,
                                 final boolean runBvCase,
                                 final boolean waitForFulfillment,
                                 final int timeout,
                                 final boolean isSVTMode,
                                 String bvAction,
                                 boolean needToLogInATP,
                                 Map<String, String> tcpDumpParams,
                                 boolean runValidation,
                                 boolean runStepByStep,
                                 BigInteger projectId,
                                 boolean standalone,
                                 UUID projectUuid) throws Exception {
        MdcUtils.put(MdcField.CALL_CHAIN_ID.toString(), callChainId);
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        final UISendRun uiSendRun = new UISendRun();
        CallChain entity = CoreObjectManager.getInstance().getManager(CallChain.class).getById(callChainId);
        if (entity == null) {
            uiSendRun.setStatus(format);
            return uiSendRun;
        }
        Collection<? extends Environment> result = CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, SearchByParameterAndProjectIdManager.class)
                .getByNameAndProjectId(environment, projectId);
        Environment env = Iterables.getFirst(result, null);
        Preconditions.checkNotNull(env, "No environment with name [" + environment + "] found");
        CallChainInstance instance = callChainExecutorManager.prepare(
                new CallchainExecutionData(callChainId, environment, environmentId, datasetId, dataset, customDataset,
                        runBvCase, runStepByStep, bvAction, needToLogInATP, tcpDumpParams, runValidation,
                        projectId, projectUuid), false);
        instance.getContext().setProjectId(projectId);
        instance.getContext().setProjectUuid(projectUuid);
        TcContext tc = instance.getContext().tc();
        tc.setProjectId(projectId);
        tc.setProjectUuid(projectUuid);
        tc.setPartNum(TCContextService.getCurrentPartitionNumberByProject(projectUuid));
        tc.setStartedFrom(StartedFrom.ITF_UI);
        tc.put("DATASET_NAME", dataset);
        tc.setRunStepByStep(runStepByStep);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), tc.getID().toString());
        RunCallChainController.StartChainSubscriber subscriber =
                new RunCallChainController.StartChainSubscriber(instance.getID());
        eventBusProvider.register(subscriber);
        try {
            ExecutionServices.getCallChainExecutorService().executeInstance(instance, waitForFulfillment);
        } catch (Throwable ex) {
            // Unregistering is not in the 'finally' block because subscriber is used below
            LOGGER.error("CallChain '{}' [id={}] execution failed", instance.getName(), instance.getID(), ex);
            eventBusProvider.unregister(subscriber);
            throw new CallChainExecutionException(ex.getMessage());
        }
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldThreadName + "/startTc[" + instance.getContext().getTC().getID() + "]");
        if (!waitForFulfillment) {
            if (timeout > 0) {
                long nanos = TimeUnit.MILLISECONDS.toNanos(250);
                long maxNanos = TimeUnit.SECONDS
                        .toNanos(timeout);
                long accumulatedNanos = 0L;
                while (subscriber.getReportLinks() == null && accumulatedNanos < maxNanos) {
                    LockSupport.parkNanos(this, nanos);
                    accumulatedNanos += nanos;
                }
                LOGGER.info("Wait for normal start of instance [{}]: {} (ms)", instance.getID(),
                        TimeUnit.NANOSECONDS.toMillis(accumulatedNanos));
            }
        }
        Thread.currentThread().setName(oldThreadName);
        if (!isSVTMode) {
            boolean noReportLinks = (subscriber.getReportLinks() == null);
            eventBusProvider.unregister(subscriber);
            if (noReportLinks) {
                throw new CallChainExecutionException(
                        "Call Chain isn't started within " + timeout + " seconds. Call Chain '"
                                + entity.getName() + "' #" + entity.getID()
                );
            } else {
                setStatus(uiSendRun, REQUESTED_FORMAT, entity, dataset);
                fillReportLinks(subscriber.getReportLinks(), projectId, projectUuid, tc, standalone);
                return fillUISendRun(uiSendRun, true, entity.getName(), subscriber.getReportLinks());
            }
        } else {
            RunCallChainController.FinishTCContextSubscriber finishSubscriber =
                    new RunCallChainController.FinishTCContextSubscriber(tc.getID());
            eventBusProvider.register(finishSubscriber);
            boolean isFinish = finishSubscriber.isFinish();
            eventBusProvider.unregister(subscriber);
            eventBusProvider.unregister(finishSubscriber);
            if (isFinish) {
                throw new CallChainExecutionException(String.format(NOT_EXECUTED_FORMAT + " (dataset: %s)",
                        entity.getName(), dataset));
            } else {
                uiSendRun.setStatus(EXECUTED_STATUS);
                uiSendRun.setKeys(tc.getBindingKeys());
                return fillUISendRun(uiSendRun, true, entity.getName(), subscriber.getReportLinks());
            }
        }
    }

    private void fillReportLinks(Map<String, String> reportLinks, BigInteger projectId, UUID projectUuid,
                                 TcContext tccontext, boolean standalone) {
        if (tccontext.isNeedToReportToItf()) {
            reportLinks.put("ITF context link", reportLinkCollector.getLinkToObject(
                    projectId,
                    projectUuid,
                    tccontext.getID(),
                    "#/context/",
                    standalone
            ));
        }

        StepContainer stepContainer = tccontext.getInitiator().getStepContainer();
        if (stepContainer instanceof Situation) {
            reportLinks.put("System link", reportLinkCollector.getLinkToObject(
                    projectId,
                    projectUuid,
                    stepContainer.getParent().getParent().getID(),
                    "#/system/",
                    standalone
            ));
        } else {
            reportLinks.put("Callchain link", reportLinkCollector.getLinkToObject(
                    projectId,
                    projectUuid,
                    stepContainer.getID(),
                    "#/callchain/",
                    standalone
            ));
        }
    }

    private void setStatus(UISendRun uiSendRun, String format, CallChain entity, String dataset) {
        uiSendRun.setStatus(String.format(format, entity.getName(), dataset));
    }

    private String concatWithSlashIfNeeded(String left, String right) {
        if (left.isEmpty()) {
            return right;
        } else if (right.isEmpty()) {
            return "";
        } else if (left.endsWith("/")) {
            return (right.startsWith("/"))
                    ? right.substring(1)
                    : right;
        } else {
            return (right.startsWith("/"))
                    ? right
                    : "/" + right;
        }
    }

    public abstract static class AbstractSubscriber {

        private final Object instanceId;
        private final Thread thread;

        AbstractSubscriber(Object instanceId) {
            this.instanceId = instanceId;
            thread = Thread.currentThread();
        }
    }

    public static class StartChainSubscriber extends AbstractSubscriber {

        private Map<String, String> reportLinks;

        StartChainSubscriber(Object instanceId) {
            super(instanceId);
        }

        @Subscribe
        public void process(CallChainEvent.Start event) {
            if (event.getInstance().getID().equals(super.instanceId)) {
                reportLinks = event.getReportLinks();
                LockSupport.unpark(super.thread);
            }
        }

        public Map<String, String> getReportLinks() {
            return reportLinks;
        }
    }

    public static class FinishTCContextSubscriber extends AbstractSubscriber {

        private boolean isFinish;

        FinishTCContextSubscriber(Object instanceId) {
            super(instanceId);
        }

        @Subscribe
        public void process(TcContextEvent.Finish event) {
            if (event.getID().equals(super.instanceId)) {
                isFinish = event.isFinish();
                LockSupport.unpark(super.thread);
            }
        }

        public boolean isFinish() {
            return isFinish;
        }
    }
}
